import sygus.Main.RankedProgram
import sygus.Main
import ast.ASTNode
import sygus.SygusFileTask
import org.apache.commons.cli.{Options,DefaultParser,CommandLineParser}

object Solutions {
  var file = "src/test/benchmarks/solutions.txt"
  lazy val solutions = scala.io.Source.fromFile(file).getLines().map(line =>
    (line.substring(0,line.indexOf(' ')),line.substring(line.indexOf(' ') + 1))).toList.groupBy(_._1).toList.map(pair => (pair._1,pair._2.map(le => le._2))).toMap

}

object BenchmarksHarness extends App {
  def runBenchmarks(dirname: String,
                    filenameToGoldStandard: String => String,
                    resultPrinter: (List[RankedProgram], List[ASTNode], Long) => String
                   ): List[String] = {
    val dirFiles = new java.io.File(dirname).listFiles().toList
    val numFiles = dirFiles.length
    println(s"Testing $dirname")
    var nextTick = 5
    for ((file,idx) <- dirFiles.zipWithIndex) yield {
      val t0 = System.currentTimeMillis()
      val programs = Main.synthesize(file.getAbsolutePath)
      val t1 = System.currentTimeMillis()
      val origFilename = filenameToGoldStandard(file.getName)
      val task = new SygusFileTask(scala.io.Source.fromFile(file).mkString)
      val goldStandard = Solutions.solutions.withDefaultValue(Nil)(origFilename).map{ solution =>
        Main.interpret(task,solution)
      }
      val percentageDone = (idx * 100)/numFiles
      if (percentageDone > nextTick) {
        println(s"$percentageDone%")
        nextTick = Math.ceil(percentageDone / 5.0).toInt * 5
      }
      file.getName + resultPrinter(programs.toList,goldStandard,t1 - t0)
    }
  }

  val options = new Options()
  options.addOption("e","easy",false,"Run the 'easy' benchmark set")
  options.addOption("h","hard", false, "Run the 'hard' benchmark set")
  options.addOption("n","nosolution",false,"Run the 'no solution' benchmark set")
  options.addOption("o","overfitted",false,"run the 'overfitted' benchmark set")

  val parser = new DefaultParser
  val cmd = parser.parse(options, args)

  val all = cmd.getOptions.isEmpty //if no option provided, run all
  val orig = all || cmd.hasOption('e')
  val contradiction = all || cmd.hasOption('n')
  val garbage = all || cmd.hasOption('o')
  val toohard = all || cmd.hasOption('h')

  val regularBenchmarkPrinter: (List[RankedProgram], List[ASTNode], Long) => String = { (programs,goldStandard, msec) =>
//    if (programs.isEmpty) Console.RED + " NO RESULTS" + Console.RESET else if (goldStandard.exists(gold => gold.code == programs.head.program.code))
//      " PASSED"
//    else Console.RED + programs.zipWithIndex.dropWhile{case (RankedProgram(tree,rate),idx) => !goldStandard.exists(gold => gold.code == tree.code)}.headOption.map{case (RankedProgram(tree,rate),idx) =>
//      " " + tree.code + " " + rate + " (" + idx + ")" + " [" + programs.head.program.code + " " + programs.head.rank + "]"}.getOrElse(" NOT FOUND")  + Console.RESET
    val idx = programs.zipWithIndex.filter{case (program,idx) => goldStandard.exists(gold => gold.code == program.program.code)}.headOption.map(_._2.toString).getOrElse("")
    "," + idx + "," + msec
  }
  val origBenchmarks: List[String] = if (orig) runBenchmarks("src/test/benchmarks/syguscomp",identity,regularBenchmarkPrinter) else Nil

  val contradictionBenchmarks = if (contradiction) runBenchmarks("src/test/benchmarks/modified_benchmarks/contradiction", filename => filename.dropRight(5) + ".sl",regularBenchmarkPrinter) else Nil

  val garbageBenchmarks = if (garbage)  runBenchmarks("src/test/benchmarks/modified_benchmarks/returns_garbage", filename => filename.dropRight(5) + ".sl",regularBenchmarkPrinter) else Nil

  val tooHardBenchmarks = if (toohard) runBenchmarks("src/test/benchmarks/too-hard",identity, { (_origPrograms,goldStandard, msec) =>
    val programs = _origPrograms.take(5)

    def goldSimSorter (gold1: (ASTNode,Int),gold2: (ASTNode,Int)): Boolean = (gold1,gold2) match {
      case ((goldProg1,sim1),(goldProg2,sim2)) => if (sim1 == sim2) goldProg1.terms < goldProg2.terms
      else sim1 > sim2
    }
    val progsWithClosest = programs.map(program =>
      goldStandard.map(goldProg => (goldProg,ast.SimilarityMetric.compute(program.program,goldProg))).sortWith(goldSimSorter).head
    )
    val topSolution = programs.headOption.map(_.program)
    val closestToTop = progsWithClosest.headOption
    val bestSolution = /*programs.zipWithIndex.map(prog => (prog._1.program,goldStandard.map(goldProg =>
      (goldProg,ast.SimilarityMetric.compute(prog._1.program,goldProg))).sortWith(goldSimSorter).head,
      prog._2)
    ).sortWith{case ((prog1, gold1, idx1),(prog2, gold2, idx2)) => goldSimSorter(gold2,gold2)}.headOption*/
      progsWithClosest.zipWithIndex.sortWith{case ((gold1,idx1),(gold2,idx2)) => goldSimSorter(gold1,gold2)}.headOption
    List(
      //gold standard data:
       goldStandard.length//# solutions
      ,goldStandard.map(_.height).sum.toDouble / goldStandard.length// average height
      ,goldStandard.map(_.terms).sum.toDouble / goldStandard.length// average terms
      //solution at rank 1 (topSolution)
      ,topSolution.map(_.height).getOrElse(0)// height
      ,topSolution.map(_.terms).getOrElse(0)// terms
      ,closestToTop.map(_._2).getOrElse(0)// distance (terms)
      ,closestToTop.map{case (gold,sim) => sim.toDouble / gold.terms}.getOrElse(0)// % of closest solution
      //best solution
      ,bestSolution.map(_._2).getOrElse("N/A")// rank
      ,bestSolution.map{case ((gold,sim),idx) => programs(idx).program.height}.getOrElse(0)// height
      ,bestSolution.map{case ((gold,sim),idx) => programs(idx).program.terms}.getOrElse(0)// terms
      ,bestSolution.map(_._1._2).getOrElse("N/A")// distance (terms)
      ,bestSolution.map{case ((gold,sim),idx) => sim.toDouble / gold.terms}.getOrElse(0)// % of closest solution
      //averages
      ,if (programs.isEmpty) 0 else (progsWithClosest.map(_._2).sum.toDouble / programs.length)// distance
      ,if (programs.isEmpty) 0 else (progsWithClosest.map{case (gold,sim) => sim.toDouble / gold.terms}.sum / programs.length)// % of closest

    ).mkString(",",",","")
  }) else Nil

  if (orig) {
    println("Easy benchmarks:")
    println("name,index of solution,time(msec)")
    origBenchmarks.foreach(println)
    println
  }
  if (contradiction) {
    println("Modified benchmarks - no solution:")
    println("name,index of solution,time(msec)")
    contradictionBenchmarks.foreach(println)
    println
  }
  if (garbage) {
    println("Modified benchmarks - overfitted:")
    println("name,index of solution,time(msec)")
    garbageBenchmarks.foreach(println)
    println
  }
  if (toohard) {
    println("Hard benchmarks:")
    println("# solutions, gold standard average height, gold standard average terms, rank 1 height, rank 1 terms, rank 1 distance to GS (terms), rank 1 % of closest solution, best rank, best height, best terms, best distance to GS (terms),% of closest solution,avg distance, avg % of closest")
    tooHardBenchmarks.foreach(println)
    println
  }
}