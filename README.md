## CloudFormation Resource Provider Package for Shield
CloudFormation Resource Provider Package for Amazon Shield

### Sync brazil package with GitHub repo
```
GITHUB_REPO_NAME="aws-cloudformation-resource-providers-shield"
git remote add upstream "git@github.com:aws-cloudformation/$GITHUB_REPO_NAME.git"
git fetch upstream
git checkout -b main upstream/main
git push origin main

git rebase mainline
git checkout mainline
git merge upstream/main
cr
```

### SAM testing (Optional):
* install [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html) ([What is SAM?](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html))
* install [Docker](https://www.docker.com/)

#### For running SAM-tests
`sam local invoke TestEntrypoint --event sam-tests/list.json`

## License

This project is licensed under the Apache-2.0 License.

