# AWS::Shield::Protection ApplicationLayerAutomaticResponseConfiguration

The automatic application layer DDoS mitigation settings for a Protection. This configuration determines whether Shield Advanced automatically manages rules in the web ACL in order to respond to application layer events that Shield Advanced determines to be DDoS attacks.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#action" title="Action">Action</a>" : <i><a href="applicationlayerautomaticresponseconfiguration.md">ApplicationLayerAutomaticResponseConfiguration</a></i>,
    "<a href="#status" title="Status">Status</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#action" title="Action">Action</a>: <i><a href="applicationlayerautomaticresponseconfiguration.md">ApplicationLayerAutomaticResponseConfiguration</a></i>
<a href="#status" title="Status">Status</a>: <i>String</i>
</pre>

## Properties

#### Action

_Required_: Yes

_Type_: <a href="applicationlayerautomaticresponseconfiguration.md">ApplicationLayerAutomaticResponseConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Status

Indicates whether automatic application layer DDoS mitigation is enabled for the protection.

_Required_: Yes

_Type_: String

_Allowed Values_: <code>ENABLED</code> | <code>DISABLED</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

