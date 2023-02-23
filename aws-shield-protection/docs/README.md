# AWS::Shield::Protection

Enables AWS Shield Advanced for a specific AWS resource. The resource can be an Amazon CloudFront distribution, Amazon Route 53 hosted zone, AWS Global Accelerator standard accelerator, Elastic IP Address, Application Load Balancer, or a Classic Load Balancer. You can protect Amazon EC2 instances and Network Load Balancers by association with protected Amazon EC2 Elastic IP addresses.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::Shield::Protection",
    "Properties" : {
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#resourcearn" title="ResourceArn">ResourceArn</a>" : <i>String</i>,
        "<a href="#healthcheckids" title="HealthCheckIds">HealthCheckIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#applicationlayerautomaticresponseconfiguration" title="ApplicationLayerAutomaticResponseConfiguration">ApplicationLayerAutomaticResponseConfiguration</a>" : <i><a href="applicationlayerautomaticresponseconfiguration.md">ApplicationLayerAutomaticResponseConfiguration</a></i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::Shield::Protection
Properties:
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#resourcearn" title="ResourceArn">ResourceArn</a>: <i>String</i>
    <a href="#healthcheckids" title="HealthCheckIds">HealthCheckIds</a>: <i>
      - String</i>
    <a href="#applicationlayerautomaticresponseconfiguration" title="ApplicationLayerAutomaticResponseConfiguration">ApplicationLayerAutomaticResponseConfiguration</a>: <i><a href="applicationlayerautomaticresponseconfiguration.md">ApplicationLayerAutomaticResponseConfiguration</a></i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
</pre>

## Properties

#### Name

Friendly name for the Protection.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>128</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### ResourceArn

The ARN (Amazon Resource Name) of the resource to be protected.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>2048</code>

_Pattern_: <code>^arn:aws.*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### HealthCheckIds

The unique identifier (ID) for the Route 53 health check that's associated with the protection.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ApplicationLayerAutomaticResponseConfiguration

The automatic application layer DDoS mitigation settings for a Protection. This configuration determines whether Shield Advanced automatically manages rules in the web ACL in order to respond to application layer events that Shield Advanced determines to be DDoS attacks.

_Required_: No

_Type_: <a href="applicationlayerautomaticresponseconfiguration.md">ApplicationLayerAutomaticResponseConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

One or more tag key-value pairs for the Protection object.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ProtectionArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ProtectionId

The unique identifier (ID) of the protection.

#### ProtectionArn

The ARN (Amazon Resource Name) of the protection.

