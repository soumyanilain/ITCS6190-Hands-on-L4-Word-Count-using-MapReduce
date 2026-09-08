# Setting up Java and Maven

You need a **JDK** and **Maven** on your own machine to build the project, and **Docker
Desktop** to run the cluster. This page covers Java and Maven; for Docker see
<https://docs.docker.com/get-started/get-docker/>.

## Which Java version

Install **Eclipse Temurin 17 (LTS)**. Temurin is a free, community-supported build of
OpenJDK.

You do not need Java 8 on your machine, even though the cluster runs Java 8 internally.
`pom.xml` sets `maven.compiler.release` to 8, so a newer JDK still produces bytecode the
cluster can load. Java 11 and 21 work too if you already have one of them.

Skip to [verifying](#verify-your-setup) if you already have a JDK and Maven — you may have
nothing to install.

---

## macOS

The quickest route is [Homebrew](https://brew.sh):

```bash
brew install --cask temurin@17
brew install maven
```

Homebrew puts both on your `PATH`, so there is nothing further to configure. If `java` is
still not found afterwards, open a new terminal.

Without Homebrew, download the macOS `.pkg` from
<https://adoptium.net/temurin/releases/> and run it, then follow the manual Maven steps in
the Linux section below.

---

## Linux

On Debian or Ubuntu:

```bash
sudo apt update
sudo apt install openjdk-17-jdk maven
```

On Fedora or RHEL:

```bash
sudo dnf install java-17-openjdk-devel maven
```

Both put `java` and `mvn` on your `PATH` automatically.

---

## Windows

### 1. Install the JDK

1. Go to <https://adoptium.net/temurin/releases/>.
2. Choose **Operating System: Windows**, **Architecture: x64**,
   **Package Type: MSI**, **Version: 17 (LTS)**.
3. Download and run the `.msi`.
4. On the **Custom Setup** screen, click the small hard-drive icon next to
   **Set JAVA_HOME variable** and choose *Will be installed on local hard drive*. Do the
   same for **Add to PATH**.

   This is the step people miss. Ticking both means the installer sets your environment
   variables for you, and you can skip most of section 3 below.

5. Finish the installation.

### 2. Install Maven

Maven has no installer — you download a zip and extract it.

1. Go to <https://maven.apache.org/download.cgi>.
2. Under **Files**, download the **Binary zip archive**, named something like
   `apache-maven-3.9.9-bin.zip`.
3. Create the folder `C:\Program Files\maven`.
4. Extract the zip into it, so you end up with
   `C:\Program Files\maven\apache-maven-3.9.9` containing `bin`, `conf` and `lib`.

### 3. Set the environment variables

The JDK installer handles `JAVA_HOME` if you ticked the boxes in step 1.4. Maven always
needs doing by hand.

1. Press the Windows key, type **Edit the system environment variables**, and open it.
2. Click **Environment Variables…**.
3. Under **System variables**, click **New…** and add:

   | Variable name | Variable value |
   | ------------- | -------------- |
   | `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot` |
   | `MAVEN_HOME` | `C:\Program Files\maven\apache-maven-3.9.9` |

   Use **Browse Directory…** rather than typing, so the version numbers are right. Yours
   will not match the examples exactly.

4. Select the `Path` variable, click **Edit…**, then **New**, and add these two lines:

   ```
   %JAVA_HOME%\bin
   %MAVEN_HOME%\bin
   ```

   Referring to `%JAVA_HOME%` rather than the full path means that when you upgrade Java
   later, you change one variable and `Path` follows.

5. Click **OK** on every window to save.

---

## Verify your setup

**Open a new terminal** — environment variable changes do not reach terminals that were
already open. Then:

```bash
java -version
mvn -version
docker --version
```

You should see something like:

```
openjdk version "17.0.13" 2024-10-15
Apache Maven 3.9.9
Docker version 27.3.1
```

The exact numbers do not matter. What matters is that all three commands run.

`mvn -version` also prints the JDK it will use. If that line points somewhere unexpected,
`JAVA_HOME` is set to a different Java than the one on your `PATH`.

---

## If something goes wrong

- **`java` or `mvn` is not recognised** — almost always a terminal opened before the `PATH`
  change. Close it and open a new one. If it persists, recheck step 3.
- **`mvn -version` reports a different Java than `java -version`** — Maven follows
  `JAVA_HOME`. Point it at your Temurin 17 installation.
- **`JAVA_HOME` is set but Maven still complains** — check the path has no trailing
  backslash and points at the JDK folder itself, the one containing `bin`, not at
  `bin`.
- **On macOS, `brew` is not found** — install Homebrew from <https://brew.sh> first.
- **Corporate or campus machine without admin rights** — use the `.zip` JDK from Adoptium
  rather than the `.msi`, extract it somewhere in your user folder, and set `JAVA_HOME` as
  a *user* variable instead of a system one.
