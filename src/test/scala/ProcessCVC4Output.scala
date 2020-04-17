object ProcessCVC4Output extends App{
  val file = args(0) //"src/test/benchmarks/modified_benchmarks/cvc4tests_time.out" //cvc4tests.out"
  if (args.length == 2) Solutions.file = args(1)
  val lines = scala.io.Source.fromFile(file).getLines().toList
  val separators = -1 +:lines.zipWithIndex.filter{case (str,idx) => str.startsWith("--")}.map(_._2)
  val resultRegex = """\s*\(define-fun\s+f\s*\((\([A-Za-z0-9_\s]+\)\s*)*\)\s+[A-Za-z]+\s*(.*)\)""".r
  for (window <- separators.sliding(2)) {
    val from = window.head
    val to = window(1)
    val segment = lines.slice(from + 1, to)
    val filename = segment.head
    val time = if (segment.last.last == 's') segment.last.dropRight(1) else segment.last
    val programs = (for (l <- segment.drop(1).dropRight(1); if !l.trim().isEmpty && l.trim() != "unsat") yield
      l match {
        case resultRegex(_,func) => func
      }).distinct

    val goldStandard = Solutions.solutions(filename.dropRight(5) + ".sl")
    println(List(
      filename,
      programs.zipWithIndex.filter{case (program,idx) =>
          goldStandard.contains(program)
      }.headOption.map(_._2).getOrElse(" "),
      time
    ).mkString(","))
  }

}
