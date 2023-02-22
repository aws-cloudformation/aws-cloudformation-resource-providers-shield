# AWS::Shield::ProtectionGroup

A grouping of protected resources so they can be handled as a collective. This resource grouping improves the accuracy of detection and reduces false positives.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::Shield::ProtectionGroup",
    "Properties" : {
        "<a href="#protectiongroupid" title="ProtectionGroupId">ProtectionGroupId</a>" : <i>String</i>,
        "<a href="#aggregation" title="Aggregation">Aggregation</a>" : <i>String</i>,
        "<a href="#pattern" title="Pattern">Pattern</a>" : <i>String</i>,
        "<a href="#members" title="Members">Members</a>" : <i>[ String, ... ]</i>,
        "<a href="#resourcetype" title="ResourceType">ResourceType</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::Shield::ProtectionGroup
Properties:
    <a href="#protectiongroupid" title="ProtectionGroupId">ProtectionGroupId</a>: <i>String</i>
    <a href="#aggregation" title="Aggregation">Aggregation</a>: <i>String</i>
    <a href="#pattern" title="Pattern">Pattern</a>: <i>String</i>
    <a href="#members" title="Members">Members</a>: <i>
      - String</i>
    <a href="#resourcetype" title="ResourceType">ResourceType</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
</pre>

## Properties

#### ProtectionGroupId

The name of the protection group. You use this to identify the protection group in lists and to manage the protection group, for example to update, delete, or describe it.

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>36</code>

_Pattern_: <code>[a-zA-Z0-9\-]*</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Aggregation

Defines how AWS Shield combines resource data for the group in order to detect, mitigate, and report events.
* Sum - Use the total traffic across the group. This is a good choice for most cases. Examples include Elastic IP addresses for EC2 instances that scale manually or automatically.
* Mean - Use the average of the traffic across the group. This is a good choice for resources that share traffic uniformly. Examples include accelerators and load balancers.
* Max - Use the highest traffic from each resource. This is useful for resources that don't share traffic and for resources that share that traffic in a non-uniform way. Examples include Amazon CloudFront and origin resources for CloudFront distributions.

_Required_: Yes

_Type_: String

_Allowed Values_: <code>SUM</code> | <code>MEAN</code> | <code>MAX</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Pattern

The criteria to use to choose the protected resources for inclusion in the group. You can include all resources that have protections, provide a list of resource Amazon Resource Names (ARNs), or include all resources of a specified resource type.

_Required_: Yes

_Type_: String

_Allowed Values_: <code>ALL</code> | <code>ARBITRARY</code> | <code>BY_RESOURCE_TYPE</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Members

The Amazon Resource Names (ARNs) of the resources to include in the protection group. You must set this when you set `Pattern` to `ARBITRARY` and you must not set it for any other `Pattern` setting.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ResourceType

The resource type to include in the protection group. All protected resources of this type are included in the protection group. Newly protected resources of this type are automatically added to the group. You must set this when you set `Pattern` to `BY_RESOURCE_TYPE` and you must not set it for any other `Pattern` setting.

_Required_: No

_Type_: String

_Allowed Values_: <code>CLOUDFRONT_DISTRIBUTION</code> | <code>ROUTE_53_HOSTED_ZONE</code> | <code>ELASTIC_IP_ALLOCATION</code> | <code>CLASSIC_LOAD_BALANCER</code> | <code>APPLICATION_LOAD_BALANCER</code> | <code>GLOBAL_ACCELERATOR</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

One or more tag key-value pairs for the Protection object.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ProtectionGroupArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ProtectionGroupArn

The ARN (Amazon Resource Name) of the protection group.

