# bester

This code is the implementation for the paper [Perfect is the Enemy of Good: Best-Effort Program Synthesis (ECOOP 2020)](http://cseweb.ucsd.edu/~hpeleg/ecoop2020.pdf).
It is released under [CRAPL--the Community Research and Academic Programming License](http://matt.might.net/articles/crapl/).

## build

Requires `sbt`. To build, run

```sbt compile```

from the root directory, and to run unit test suite, run

```sbt test```

We recommend assembling bester using

```sbt assembly```

## run

Bester has several main classes which can be displayed and run via `sbt run`, and run from the assembled jar file:

sygus.Main: run with `-f <sl filename>` to synthesize a solution to the SyGuS task in the sl file

pcShell.ShellMain: run with `-f <sl filename>` to start an interactive shell that allows editing programs and synthesizing a solution to the SyGuS task in the sl file
