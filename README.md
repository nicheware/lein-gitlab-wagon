# lein-gitlab-wagon

Enables Clojure Leiningen projects to deploy and consume artifacts in private GitLab maven repositories.

Current version: [lein-gitlab-wagon "1.0.0"]

## Context
### Problem

GitLab supports private maven repositories. To authenticate to these you must supply a HTTP header in each repository request as documented on the [GitLab site](https://docs.gitlab.com/ee/user/project/packages/maven_repository.html).

The header specifies either a `Private-Token` and user token value or `Job-Token` and a token provided by the GitLab CI/CD process.

Maven based projects using the standard HTTP wagon can provide this authentication by configuring HTTP headers for setting tokens, rather than username password.

However Clojure projects using [Leiningen](https://github.com/technomancy/leiningen) cannot configure HTTP headers for repository authentication. Repository authentication accepts  only username, password and passphrase and uses this to set standard HTTP Authentication.

### Solution

The `lein-gitlab-wagon` is a wrapper around the standard HTTP wagon. It will accept a configured username and password but treat them as token type ("http header name") and token value ("http header value").

## Usage

### Leiningen 2.x

Add the plugin and repositories listing to your `project.clj`.

```clj
:plugins [[lein-gitlab-wagon "1.0.0"]]
```

You can specify the GitLab token type (`Private-Token` or `Job-Token`) using `:username`.

You can specify the token value (Personal token or CI/CD token) using  `:password`.

`:username` and `:password` are defined using any of the standard Leiningen authentication configuration techniques.

In order to trigger the use of this repository wagon you need to replace the `https:` scheme in your Gitlab URL with `gitlab:`. At execution time, the wagon will still use `https` to communicate with the repository.

#### Store credentials under arbitrary environment variables

```clojure
:repositories {"releases"  {:url           "gitlab://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven"
                            :username      "Job-Token"
                            :password      :env/ci_job_token
                            :sign-releases false}

               "snapshots" {:url          "gitlab://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven"
                            :username      "Private-Token"
                            :password      :env/gitlab_private_token}}
```

#### Store credentials in an encrypted file

Add the following to `project.clj`:

```clojure
:repositories [["private" {:url "gitlab://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven" :creds :gpg}]]
```

And in `~/.lein/credentials.clj.gpg`:

```
 {"gitlab://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven" {:username "Private-Token"
                                                                   :password "98b0b104ca1211e19a6c" ;; Your gitlab private token
                                                                  }}
```

The map key here can be either a string for an exact match or a regex checked against the repository URL if you have the same credentials for multiple repositories.

See `lein help deploying` for additional details on storing credentials.

### Maven

There should be no need to use this in a maven project, as the standard wagon with `<httpHeaders>` configuration in settings.xml described above should work. But for completeness, the following will also work.

#### pom.xml

```xml
     <build>
        <extensions>
            <extension>
                <groupId>nicheware</groupId>
                <artifactId>lein-gitlab-wagon</artifactId>
                <version>1.0.0</version>
            </extension>
        </extensions>
    </build>

    <!-- to publish to a private bucket -->

    <distributionManagement>
        <repository>
            <id>someId</id>
            <name>Some Name</name>
            <url>gitlab://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven</url>
        </repository>
        <snapshotRepository>
            <id>someSnapshotId</id>
            <name>Some Snapshot Name</name>
            <url>gitlab://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven</url>
        </snapshotRepository>
     </distributionManagement>

    <!-- get this plugin from clojars -->

     <pluginRepositories>
       <pluginRepository>
         <id>clojars.org</id>
         <name>Clojars Repository</name>
         <url>http://clojars.org/repo</url>
       </pluginRepository>
     </pluginRepositories>

     <!-- to consume artifacts from a private bucket -->

     <repositories>
        <repository>
            <id>someId</id>
            <name>Some Name</name>
            <url>gitlab://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven</url>
        </repository>
     </repositories>
```

#### settings.xml

This xml is needed to set the token used for authentication.

```xml


<settings>
    <servers>
        <server>
            <!-- you can actually put the key and secret in here, I like to get them from the env -->
            <id>someId</id>
            <username>Job-Token</username>
            <password>${env.CI_JOB_TOKEN}</password>
        </server>
    </servers>
</settings>

```


## Releasing this library

```
# Make sure all of the versions are as you want them
git tag v1.x.y
git push --tags
mvn deploy
# Bump to the next SNAPSHOT version
```

## License

Copyright © 2019 Nicheware Solutions Pty Ltd

Based on [s3-wagon-private](https://github.com/s3-wagon-private/s3-wagon-private)

Distributed under the Apache Public License version 2.0.

[chained-provider-class]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html
[credentials-file-format]: http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#aws-credentials-file-format
