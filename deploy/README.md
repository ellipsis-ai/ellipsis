# Overview
To deploy to Production or Stag01 simply push to the prod or stag01 branches.

The script circleci_build is a Bash script that package the application into a
jar file then uploads the assets to the environment assets buckets and finally
create a Beanstalk App version for the selected environment.

# What to do when I update the circleci_build script?
- Test the new script  :-)
- Upload the new script to Dockerhub so that CircleCi can pull the new version.
  - Repo: https://hub.docker.com/r/ellipsis/circleci_primary/
  - Procedure:
    - $ docker login
    - $ docker images
    - $ docker tag <IMAGE_ID> ellipsis/circleci_primary:<TAG>
    - $ docker push ellipsis/circleci_primary

    Remember to update the latest tag
    - $ docker tag <IMAGE_ID> ellipsis/circleci_primary:latest
    - $ docker push ellipsis/circleci_primary
