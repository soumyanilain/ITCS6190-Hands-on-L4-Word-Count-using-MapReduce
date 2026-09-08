# Hands-on L4 — Report

**Name:** Soumyanil Ain
**Student ID:** 801488534
**Email:** sain@charlotte.edu

---

## What I ran

I followed the steps in the README without deviating. Setup was on Windows 11 with Docker
Desktop, JDK 21 (Temurin) and Maven 3.9.16, so the host commands below use Windows
backslash paths.

On the host:

```bash
docker --version
java -version
mvn -version

docker compose up -d
docker ps

mvn clean package

docker cp target\WordCountUsingHadoop-0.0.1-SNAPSHOT.jar resourcemanager:/tmp/
docker cp shared-folder\input\data\input.txt resourcemanager:/tmp/

docker exec -it resourcemanager bash
```

Inside the resourcemanager container:

```bash
cd /tmp

hadoop fs -mkdir -p /input/data
hadoop fs -put ./input.txt /input/data
hadoop fs -ls /input/data

hadoop jar /tmp/WordCountUsingHadoop-0.0.1-SNAPSHOT.jar \
  com.example.controller.Controller /input/data/input.txt /output

hadoop fs -cat /output/*

hdfs dfs -get /output /tmp/
exit
```

Back on the host:

```bash
docker cp resourcemanager:/tmp/output/. shared-folder\output\
docker compose down
```

---

## Input and output

### My input dataset

Four paragraphs of my own prose about Hadoop, HDFS and MapReduce. 1344 bytes, 7 lines,
stored at `shared-folder/input/data/input.txt`. I picked a topic with a lot of repeated
vocabulary so the counts would be interesting rather than a flat list of ones.

```
Hadoop is a framework for distributed storage and distributed processing of large data sets across clusters of computers. The Hadoop framework was designed so that hardware failures are handled automatically by the framework itself rather than by the application.

The core of Hadoop consists of a storage layer called HDFS and a processing layer called MapReduce. HDFS splits each file into large blocks and distributes those blocks across the nodes in a cluster. MapReduce processes data on the same nodes where the data blocks already live, which reduces the amount of data moved across the network.

A MapReduce job has two phases. The map phase reads the input data and produces intermediate key value pairs. The reduce phase collects those intermediate pairs and combines them into the final output. Between the map phase and the reduce phase, the framework groups every value that shares the same key and sends it to the same reducer. This grouping step is called the shuffle.

A Hadoop cluster has a NameNode that tracks where every block lives, and DataNodes that store the actual blocks. The ResourceManager schedules work across the cluster, and NodeManagers run the containers where the map tasks and reduce tasks actually execute. When a node fails, the framework reruns the lost tasks on another node, and the job continues.
```

### The output the job produced

Contents of `shared-folder/output/part-r-00000`:

```
the     22
and     11
data    5
The     5
framework       5
that    4
Hadoop  4
across  4
phase   3
tasks   3
blocks  3
reduce  3
same    3
map     3
called  3
where   3
large   2
job     2
layer   2
distributed     2
intermediate    2
HDFS    2
value   2
MapReduce       2
nodes   2
key     2
every   2
storage 2
processing      2
into    2
those   2
has     2
application.    1
cluster 1
DataNodes       1
step    1
This    1
reruns  1
clusters        1
collects        1
reads   1
core    1
for     1
phases. 1
distributes     1
groups  1
handled 1
amount  1
pairs.  1
shuffle.        1
reducer.        1
was     1
network.        1
MapReduce.      1
execute.        1
splits  1
containers      1
block   1
than    1
schedules       1
pairs   1
processes       1
lives,  1
grouping        1
automatically   1
fails,  1
tracks  1
live,   1
sets    1
itself  1
final   1
NodeManagers    1
designed        1
NameNode        1
failures        1
two     1
are     1
sends   1
actually        1
ResourceManager 1
moved   1
consists        1
continues.      1
phase,  1
each    1
shares  1
store   1
node    1
file    1
another 1
which   1
reduces 1
hardware        1
combines        1
work    1
cluster,        1
input   1
them    1
actual  1
blocks. 1
cluster.        1
output. 1
produces        1
lost    1
run     1
already 1
rather  1
When    1
Between 1
computers.      1
node,   1
```

---

## What I observed

The job ran as a single map task and a single reduce task. My input is 1344 bytes, far
below the 128 MB HDFS block size, so the submitter reported `number of splits:1` and only
one mapper was launched.

Map reached 100% about seven seconds after the job started running (17:38:48 to 17:38:55),
and reduce finished six seconds later at 17:39:01. Reduce stayed at 0% for the entire time
map was running. That gap is the shuffle boundary: no reducer can start until every mapper
has finished, because a reducer needs all the values for a given key before it can add them
up. In occupied slot time the split was 3795 ms for map against 2705 ms for reduce.

The counters made the shape of the job clear. `Map input records=7` is the seven lines of
my file. `Map output records=195` is every word of three or more characters, each emitted
with a count of 1. A combiner then ran on the map side: `Combine input records=195` against
`Combine output records=111`, so only 111 pairs crossed the network instead of 195.
`Reduce input groups`, `Reduce input records` and `Reduce output records` were all 111,
matching the 111 distinct words in the final output.

The counters also showed `Rack-local map tasks=1` rather than a data-local task, so the map
task did not run on a node holding the block and the data was read across the network. With
one small file that costs nothing, but it is the data-locality tradeoff that matters at
scale.

The NameNode UI at <http://localhost:9870> showed three live DataNodes throughout the run,
and `hadoop fs -ls /input/data` reported the input file with a replication factor of 2. The
ResourceManager UI at <http://localhost:8088> showed the application move from ACCEPTED to
RUNNING to FINISHED/SUCCEEDED, with one map container and one reduce container allocated.

The output ordering was descending by count, as expected. Two things in the result were
worth noticing. Punctuation is part of the token, so `blocks` (3) and `blocks.` (1) are
counted separately, as are `cluster`, `cluster,` and `cluster.`. Counting is also
case-sensitive, so `the` (22) and `The` (5) appear as separate entries. Both follow from the
mapper splitting on whitespace and doing no normalisation of the tokens it produces.

---

## Problems and fixes

Nothing went wrong during the job itself. It succeeded on the first run, and I never hit
the "output directory already exists" error since I only ran it once.

The one problem was on my own machine, during the prerequisite check:

```
C:\Users\soumy>mvn -version
'mvn' is not recognized as an internal or external command,
operable program or batch file.
```

Maven was not installed. `winget install Apache.Maven` returned "No package found matching
input criteria", so I installed it manually instead: downloaded the Maven 3.9.16 binary zip,
extracted it, set a `JAVA_HOME` user variable pointing at the JDK root
(`C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot`, not its `bin` subdirectory) and
added Maven's `bin` directory to `Path`. The check still failed until I opened a new
terminal, since a shell reads environment variables once at startup. After that
`mvn -version` reported Maven 3.9.16 running on Java 21.

I also had a false start with the repository itself. My first repository came out empty
because it was created without the template being applied, so cloning it produced an empty
folder. I deleted it, recreated it with **Use this template** on the course repository, and
confirmed the files were visible on GitHub before cloning again.
