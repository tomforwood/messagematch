# messagematch

message match expresses a specification for a JSON formatted message. It is designed to be used to match against messages generated by a system AND to generate messages for input into downstream systems, forming the contract between them.

In general it looks like a regular JSON message with some special matching keys/values as defined below. JSON in the spec that does not use any special matching is "basic" json. matching expressions always start with a $.



## Matching

For basic json the actual and expected are expected to be exact matches except 
 - keys in JSON objects are allowed to appear in any order
 - extra keys in actual value objects are ingored
 - extra values in json arrays are skipped and ignored
 - complex objects and arrays are matched recursively
The matching behaviour can be altered by using matchers. A matcher is a value or key starting with '$' e.g. `"value" : "$Int"` will match with `"value" : "5"` or `"value" : 5`

A "basic" value that starts with $ can be escaped using '\'s e.g. `"dollarValue" : "\\$100"`

## Generation

In input generating mode basic json is reproduced exactly.

For the value based matchers the matcher expression can be followed with a comma and a provided value that will be used in the generated message e.g. `"value" : "$Int,5"` will generate `"value" : 5`.


The structural type matchers (see below) are simply removed

## types of matcher

### Primitive values

#### type based
Matches any value of the appropriate primitive type. Numeric types are not subjected to upper and lower bounds e.g. $Int matches fine with 1000000000000000000000. If no value is supplied then a default will be used when generating
 - $Int - any json integer e.g. `"value" : "$Int"` will match `"value" : 5` but will generate `"value" : 0` as no default was supplied
 - $Num - any json numeric value - default generates "0.0"
 - $String - any json string value - default generates ""
 - $Instant - a value that can be parsed as an instant, as ISO 8601. Default generation is the current instant
 - $Time - a value that can be parsed as a ISO-8601 local time. E.g. 15:32:05.123. Default generation is the currenct local time
 - $Date - a value that can be parsed as an ISO-8601 local date Eg.g 2021-05-11. Default generation is the current local date
 
#### regexp
Matches based on a RegEx. The regex is delimited using '^' at the beginning and end. '^'s appearing inside the regex need be escaped. The regex must be followed with a provided value to be used for generation.

E.g. `"value" : "$^[\\^0-9]*^,abc" ` will match against any string not containing a digit and will generate `"value" : "abc"`


### comparators
A value can be compared relative to a given value  using <,>, >=,<= or +-. the +- operator is used to specify a value should be within a given margin of the centre value. The values are always treated as decimals.
E.g. 
 - `"Value" : "$<5"` matches values less than 5
 - `"Value" :"$+-(1.2,0.1)"` matches values between 1.1 and 1.3 (inclusive)
 
 When generating for +- the given centre value is used.<br>
 For the closed comparators >= and <= the given value is generated.<br>
 For the open comparators >  and < the value has 1 added or subtracted from it;

#### binding

//TODO cross message and runtime supplied bindings

A value can be *bound* using *=name* to create constraints within a json document. E.g.

	{ 
		"value1" : "$Int=myVar , 6",
		"value2" : "$>5=myVar"
	}

will ensure that the value1 and value2 have the same value, it is an integer and it is > 5

When generating the same value will be used for both with a value being generated the first time a variable is encountered. E.g. in the example above we would generate `{	"value1" : 6,"value2" : 6 }`. If a provided value has not been supplied (or a "bad" value is provided) it is possible that json is generated that does not in 

A variable that has already been bound earlier in a document can be used as a value in an expression. E.g.

	{ 
		"value1" : "$Int=myVar , 6",
		"value2" : "$>$myVar"
	}
will match if "value2" is > "value1".  If it has not been bound before being used in this way it will be a match failure. 

When generating the bound value will be generated by the usual rules, e.g. in the example above  we would generate `{	"value1" : 6,"value2" : 7 }`

#### date/time based

A few date time based variables are pre bound to be used in expressions - $Date, $Time and $Instant. The are initialised at the start of matching or generation with the current date and time (to the millisecond). E.g. 

	{
	"currentms" : "$Int=Instant"
	"currentDate" : "$<$Date",
	"startTime" : "$+-($Instant,1000)"
	}
	
will match (If the current unix time is 1636044195000ms (2021-11-04T16:42:57+00:0))
	
	{
		"currentms" : "1636044195000",
		"currentDate" : "2021-11-03",
		"startTime" : "1636044194000"
	}

Date formats as strings are expected to conform to ISO 8601. 



### plugable
TODO it would be nice to be able to define custom matchers

### Structural Values

By default the matching is forgiving of extra values. e.g. `{"value1":"$Int"}` as a matcher will match fine with `{"value1":5, "value2":6}` with the unexpected fields being simply ignored.

#### "strict" mode
If the special value`{"$Strict":"true"}` appears in a json object then any value appearing in the concrete object that is not in the matcher is considered an error. E.g. `{"$Strict":"true", "value1":"$Int"}` will not match `{"value1":5, "value2":6}` but will match `{"value1":5}`

"$Strict":""false" still sets strict mode the boolean is ignored by the implementation

#### map key mapping
Where the keys are not know before runtime e.g. the serialization of a java map the same expressions can be applied to the keys. E.g. `{"$String":"abc"}` will match `{"value":"abc"}` Where the key expression matches more than one key appearing in concrete json the matching will be attempted against all matching keys. E.g.

	{
		"$^[A-Z]{3}^,GBP" : "$Int,1000"
	}

will match against each currency in 

	{
		"GBP" : 1000,
		"USD" : 2000,
		"EUR" : 3
	}

#### list (/map) value count
The minimum and maximum number of values in an array or object can be specified with the special value `{"$size":"1-10"}` the values are inclusive and both the min and max are optional although one must be present e.g. "-10" means ten or fewer, "1-" means 1 or more.

#### arrays
By default arrays are matched index by index with the first entry in the matcher array being matched against the first entry in the concrete array etc with additional entries in the concrete array being ignored.
This behaviour can be changed by providing a special object as the first member of the array. The special object must declare some flags as its first pairs. the special object will be discarded after its flags have been read. E.g.

  [
    { "$strict":"true"},
    { "value":""$INT"}
  ]
  
will match an array containing a single object containing a "value" key.

The flags that can be set in the special object are
 - "$Strict" : "true"  -enforce that only explicitly matched values are present
 - "$Size" : "1-2" - enforce the min and max size of the array
 - "$Each" : every node in the concrete json will be matched to the single matcher node supplied
 - "$Unorderd" : "true" - allow the entries to come in a differnt order (not implemented)