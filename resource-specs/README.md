# Resource Specs

These files are generated using [this package](https://code.amazon.com/packages/CFNResourceSpecification/trees/mainline), and are used to generate documentation site. They're commited into the repo for version tracking purpose.

Command used to generate spec files:

```shell
./test-runtime/bin/convert-uluru-schemas -ptu "$HOME/Git/aws-cloudformation-resource-providers-shield/aws-shield-proactiveengagement/aws-shield-proactiveengagement.json"
./test-runtime/bin/convert-uluru-schemas -ptu "$HOME/Git/aws-cloudformation-resource-providers-shield/aws-shield-protection/aws-shield-protection.json"
./test-runtime/bin/convert-uluru-schemas -ptu "$HOME/Git/aws-cloudformation-resource-providers-shield/aws-shield-protectiongroup/aws-shield-protectiongroup.json"
./test-runtime/bin/convert-uluru-schemas -ptu "$HOME/Git/aws-cloudformation-resource-providers-shield/aws-shield-subscription/aws-shield-subscription.json"
./test-runtime/bin/convert-uluru-schemas -ptu "$HOME/Git/aws-cloudformation-resource-providers-shield/aws-shield-drtaccess/aws-shield-drtaccess.json"
```
