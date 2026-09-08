# Hands-on L4: Word Count using MapReduce

**ITCS 6190/8190 — Cloud Computing for Data Analysis — Fall 2026**

In this hands-on you run a working Hadoop MapReduce job. Everything you need is already in
this repository: the Java code, the Maven build, and a Docker Compose file that brings up a
small Hadoop cluster. **You are not writing any code.** You will build the project, start
the cluster, load your own data into HDFS, run the job, and collect the results.

By the end you should be comfortable with the cycle every MapReduce job goes through:
build, deploy, load data, submit, read output.

**Worth 1 point.** Submit the repository link on Canvas by 11:59 pm on the day of the
class. Work individually.

---

## The job you are running

The program counts how many times each word appears in a text file, ignoring words shorter
than three characters, and prints the results from most frequent to least.

### Example input

```
Hello world
Hello Hadoop
Hadoop is powerful
Hadoop is used for big data
```

### Expected output

```
Hadoop 3
Hello 2
used 1
for 1
big 1
data 1
powerful 1
world 1
```

`is` is missing because it is only two characters. Counting is case-sensitive, so `Hadoop`
and `hadoop` would be counted separately. Words with the same count may appear in any order
among themselves.

---

## What is in this repository

| Path | What it is |
| ---- | ---------- |
| `src/main/java/com/example/WordMapper.java` | reads each line, splits it into words, emits `(word, 1)` for every word of three or more characters |
| `src/main/java/com/example/WordReducer.java` | adds up the 1s for each word, then sorts the results by count before writing them out |
| `src/main/java/com/example/controller/Controller.java` | configures the job and contains `main` |
| `pom.xml` | the Maven build |
| `docker-compose.yml`, `config` | the cluster: NameNode, 3 DataNodes, ResourceManager, 2 NodeManagers, history server, all on the official `apache/hadoop:3` image |
| `shared-folder/input/data/input.txt` | **placeholder — you replace this with your own text** |
| `SETUP.md` | installing Java and Maven on Windows, macOS or Linux |

---

## Prerequisites

- **Docker Desktop**, running. See <https://docs.docker.com/get-started/get-docker/>.
- **Java (JDK)** and **Maven**. See [SETUP.md](SETUP.md) if you do not have them.

Check all three before you start:

```bash
docker --version
java -version
mvn -version
```

---

## Steps

### 1. Put your own text in the input file

Open `shared-folder/input/data/input.txt` and replace the placeholder line with text of your
own — a few paragraphs is plenty. Pick something with repeated words so the counts are
interesting.

### 2. Start the Hadoop cluster

```bash
docker compose up -d
```

Give it a minute, then open <http://localhost:9870>. You should see the NameNode with three
live DataNodes.

### 3. Build the project

```bash
mvn clean package
```

This produces `target/WordCountUsingHadoop-0.0.1-SNAPSHOT.jar`. The first build downloads
the Hadoop libraries and takes a while.

### 4. Copy the JAR into the ResourceManager container

```bash
docker cp target/WordCountUsingHadoop-0.0.1-SNAPSHOT.jar resourcemanager:/tmp/
```

### 5. Copy your dataset into the container

```bash
docker cp shared-folder/input/data/input.txt resourcemanager:/tmp/
```

### 6. Open a shell in the container

```bash
docker exec -it resourcemanager bash
cd /tmp
```

Everything from here until step 10 happens inside the container.

### 7. Load your data into HDFS

```bash
hadoop fs -mkdir -p /input/data
hadoop fs -put ./input.txt /input/data
hadoop fs -ls /input/data
```

The last command should list your file. This is the moment your data stops being a local
file and becomes blocks distributed across the DataNodes.

### 8. Run the job

```bash
hadoop jar /tmp/WordCountUsingHadoop-0.0.1-SNAPSHOT.jar \
  com.example.controller.Controller /input/data/input.txt /output
```

Watch the progress lines: map reaches 100% before reduce starts moving. That ordering is
the shuffle phase in action.

The output directory must **not** already exist. To rerun, either delete it with
`hadoop fs -rm -r /output` or write to a new path such as `/output2`.

### 9. Look at the results

```bash
hadoop fs -cat /output/*
```

### 10. Copy the results back to your machine

Inside the container:

```bash
hdfs dfs -get /output /tmp/
exit
```

Then on your machine:

```bash
docker cp resourcemanager:/tmp/output/. shared-folder/output/
```

### 11. Stop the cluster

```bash
docker compose down
```

Add `-v` if you also want to discard the HDFS data.

---

## What to commit

- Your **input dataset** at `shared-folder/input/data/input.txt`
- The **output** you collected in step 10, under `shared-folder/output/`
- Your **report** in a new file called `REPORT.md`

Leave this README as it is. It is the instructions, and it should still be here when you
submit.

---

## Report

Create a file called **`REPORT.md`** in the root of this repository. Keep it short.

### What I ran
The commands you used, in the order you used them. If you deviated from the steps above,
say where and why.

### Input and output
Your input dataset, and the output the job produced. Paste the output rather than linking
to the file.

### What I observed
A few sentences on what happened while the job ran. Pick whatever you actually noticed: how
long the map phase took compared with reduce, how many DataNodes showed as live, what the
ResourceManager at <http://localhost:8088> displayed during the run, whether the output
ordering matched what you expected.

### Problems and fixes
Anything that went wrong and what resolved it. The actual error message is worth more than
"it did not work". If nothing went wrong, say so.

---

## Submission

### 1. Make your own copy of this repository

On the repository page, click the green **Use this template** button, then
**Create a new repository**. Name it `ITCS6190-H4-<your-name>` and set the visibility to
**Public**.

Do not fork, and do not clone this repository directly. A fork or a clone still points at
the course repository, so your work would not end up anywhere we can grade it.

Then clone *your* new repository to your machine and work there.

### 2. Commit your work

Your input dataset, your output, and `REPORT.md`.

### 3. Submit the link

Post the URL of **your** repository on Canvas. Keep it public until grades are posted.
There is no need to add the instructor or the TAs as collaborators.

---

## Troubleshooting

- **`Output directory already exists`** — the most common error. See step 8.
- **`docker cp` says no such container** — the cluster is not running or is still starting.
  Run `docker ps` and look for `resourcemanager`.
- **Job is accepted but never progresses** — the NodeManagers may not have registered yet.
  Check <http://localhost:8088> and confirm there are active nodes.
- **`mvn` cannot resolve Hadoop dependencies** — the first build needs network access.
- **`UnsupportedClassVersionError` when the job runs** — your JDK compiled newer bytecode
  than the cluster can load. The cluster runs Java 8. `pom.xml` already pins
  `maven.compiler.release` to 8, so rerun `mvn clean package`; if it persists, build with
  a JDK 8 or 11.
- **`Permission denied` writing inside the container** — the image runs as the `hadoop`
  user, not root. Work in `/tmp`, as the steps above do.
- **`ClassNotFoundException: com.example.controller.Controller`** — the JAR in the
  container is stale. Rebuild, then repeat step 4 to copy it across again.
- **Output is empty** — every word in your input may be shorter than three characters, or
  the job read the wrong path. Check with `hadoop fs -ls /input/data`.
- **Ports already in use** — something else is on 9870 or 8088. Stop it, or change the port
  mappings in `docker-compose.yml`.
