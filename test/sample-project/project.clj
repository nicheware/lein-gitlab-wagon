(defproject nicheware/lein-test-project "0.0.1-SNAPSHOT"
  :license "no-license"
  :url "https://github.com/nicheware/lein-test-project"
  :description "This is a test repository"
  :plugins [[nicheware/lein-gitlab-wagon "1.0.0"]]
  :repositories [["testing" {:url "gitlab://gitlab.com/api/v4/projects/PROJECT_ID/packages/maven"
                             :username      "Private-Token"
                             :password      :env/gitlab_private_token}]])
