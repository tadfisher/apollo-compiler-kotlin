grammar Graphql;

// Adapted from the GraphQL language specification
// http://facebook.github.io/graphql/June2018/#sec-Appendix-Grammar-Summary

// Lexical tokens
Name: [_A-Za-z][_0-9A-Za-z]*;
IntValue: IntegerPart;
fragment IntegerPart
    : NegativeSign? '0'
    | NegativeSign? NonZeroDigit Digit*;
fragment NegativeSign: '-';
fragment Digit: '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9';
fragment NonZeroDigit: Digit ~'0';
FloatValue
    : IntegerPart FractionalPart
    | IntegerPart ExponentPart
    | IntegerPart FractionalPart ExponentPart;
fragment FractionalPart: '.' Digit+;
fragment ExponentPart: ExponentIndicator Sign? Digit+;
fragment ExponentIndicator: 'e' | 'E';
fragment Sign: '+' | '-';
StringValue
    : '"' StringCharacter* '"'
    | '"""' BlockStringCharacter* '"""';
fragment StringCharacter
    // : SourceCharacter ~('"' | '\\' | LineTerminator)
    : SourceCharacter ~('"' | '\\' | '\u000A' | '\u000D')
    | '\\u' EscapedUnicode
    | '\\' EscapedCharacter;
fragment EscapedUnicode: Hex Hex Hex Hex;
fragment Hex: [0-9A-Fa-f];
fragment EscapedCharacter: '"' | '\\' | '/' | 'b' | 'f' | 'n' | 'r' | 't';
// BlockStringCharacter: (SourceCharacter ~('"""' | '\\"""')) | '\\"""';
fragment BlockStringCharacter
    : (SourceCharacter | '\\"""') '""' ~'"'
    | (SourceCharacter | '\\"""') '"' ~'"'
    | (SourceCharacter | '\\"""') ~'"';
fragment SourceCharacter : '\u0009' | '\u000A' | '\u000D' | '\u0020'..'\uFFFF';

// Ignored tokens
Ignored: (UnicodeBOM | WhiteSpace | LineTerminator | Comment | Comma) -> skip;
fragment UnicodeBOM: '\uFEFF';
//WhiteSpace: [\u0009\u0020];
//fragment WhiteSpace: '\u0009' | '\u0020';
fragment WhiteSpace: [\t\u000b\f\u0020\u00a0];
//fragment LineTerminator: '\u000A' | '\u000D';
fragment LineTerminator: [\n\r\u2028\u2029];
fragment Comment: '#' CommentChar*;
// CommentChar: SourceCharacter ~LineTerminator;
fragment CommentChar: SourceCharacter ~[\u000A\u000D];
fragment Comma: ',';

document: definition+;

definition: executableDefinition | typeSystemDefinition | typeSystemExtension;

executableDefinition: operationDefinition | fragmentDefinition;

operationDefinition: selectionSet | operationType Name? variableDefinitions? directives? selectionSet;

operationType: 'query' | 'mutation' | 'subscription';

selectionSet: '{' selection+ '}';

selection: field | fragmentSpread | inlineFragment;

field : alias? Name arguments? directives? selectionSet?;

alias: Name ':';

arguments: argument+;
arguments_const: argument_const+;

argument: Name ':' value;
argument_const: Name ':' value_const;

fragmentSpread: '...' fragmentName directives?;

inlineFragment: '...' typeCondition? directives? selectionSet;

fragmentDefinition: 'fragment' fragmentName typeCondition directives? selectionSet;

fragmentName: Name ~'on';

typeCondition: 'on' namedType;

value
    : variable
    | IntValue
    | FloatValue
    | StringValue
    | booleanValue
    | nullValue
    | enumValue
    | listValue
    | objectValue;

value_const
    : IntValue
    | FloatValue
    | StringValue
    | booleanValue
    | nullValue
    | enumValue
    | listValue_const
    | objectValue_const;

booleanValue: 'true' | 'false';

nullValue: 'null';

enumValue: Name ~('true' | 'false' | 'null');

listValue: '[' ']' | '[' value+ ']';
listValue_const: '[' ']' | '[' value_const+ ']';

objectValue: '{' '}' | '{' objectField+ '}';
objectValue_const: '{' '}' | '{' objectField_const+ '}';

objectField: Name ':' value;
objectField_const: Name ':' value_const;

variableDefinitions: '(' variableDefinition+ ')';

variableDefinition: variable ':' type defaultValue?;

variable: '$' Name;

defaultValue: '=' value_const;

type: namedType | listType | nonNullType;

namedType: Name;

listType: '[' type ']';

nonNullType: namedType '!' | listType '!';

directives: directive+;
directives_const: directive_const+;

directive: '@' Name arguments?;
directive_const: '@' Name arguments_const?;

typeSystemDefinition: schemaDefinition | typeDefinition | directiveDefinition;

typeSystemExtension: schemaExtension | typeExtension;

schemaDefinition: 'schema' directives_const? '{' operationTypeDefinition+ '}';

schemaExtension
    : 'extend' 'schema' directives_const? '{' operationTypeDefinition+ '}'
    | 'extend' 'schema' directives_const;

operationTypeDefinition: operationType ':' namedType;

description: StringValue;

typeDefinition
    : scalarTypeDefinition
    | objectTypeDefinition
    | interfaceTypeDefinition
    | unionTypeDefinition
    | enumTypeDefinition
    | inputObjectTypeDefinition;

typeExtension
    : scalarTypeExtension
    | objectTypeExtension
    | interfaceTypeExtension
    | unionTypeExtension
    | enumTypeExtension
    | inputObjectTypeExtension;

scalarTypeDefinition: description? 'scalar' Name directives_const?;

scalarTypeExtension: 'extend' 'scalar' Name directives_const;

objectTypeDefinition: description? 'type' Name implementsInterfaces? directives_const? fieldsDefinition?;

objectTypeExtension
    : 'extend' 'type' Name implementsInterfaces? directives_const? fieldsDefinition
    | 'extend' 'type' Name implementsInterfaces? directives_const
    | 'extend' 'type' Name implementsInterfaces;

implementsInterfaces
    : 'implements' '&'? namedType
    | implementsInterfaces '&' namedType;

fieldsDefinition: '{' fieldDefinition+ '}';

fieldDefinition: description? Name argumentsDefinition? ':' type directives_const?;

argumentsDefinition: '(' inputValueDefinition+ ')';

inputValueDefinition: description? ':' Name type defaultValue? directives_const?;

interfaceTypeDefinition: description? 'interface' Name directives_const? fieldsDefinition?;

interfaceTypeExtension
    : 'extend' 'interface' Name directives_const? fieldsDefinition
    | 'extend' 'interface' Name directives_const;

unionTypeDefinition: description? 'union' Name directives_const? unionMemberTypes?;

unionMemberTypes
    : '=' '|'? namedType
    | unionMemberTypes '|' namedType;

unionTypeExtension
    : 'extend' 'union' Name directives_const? unionMemberTypes
    | 'extend' 'union' Name directives_const;

enumTypeDefinition: description? 'enum' Name directives_const? enumValuesDefinition?;

enumValuesDefinition: '{' enumValueDefinition+ '}';

enumValueDefinition: description? enumValue directives_const?;

enumTypeExtension
    : 'extend' 'enum' Name directives_const? enumValuesDefinition
    | 'extend' 'enum' Name directives_const;

inputObjectTypeDefinition: description? 'input' Name directives_const? inputFieldsDefinition?;

inputFieldsDefinition: '{' inputValueDefinition+ '}';

inputObjectTypeExtension
    : 'extend' 'input' Name directives_const? inputFieldsDefinition
    | 'extend' 'input' Name directives_const;

directiveDefinition: description? 'directive' '@' Name argumentsDefinition? 'on' directiveLocations;

directiveLocations
    : '|'? directiveLocation
    | directiveLocations '|' directiveLocation;

directiveLocation: executableDirectiveLocation | typeSystemDirectiveLocation;

executableDirectiveLocation
    : 'QUERY'
    | 'MUTATION'
    | 'SUBSCRIPTION'
    | 'FIELD'
    | 'FRAGMENT_DEFINITION'
    | 'FRAGMENT_SPREAD'
    | 'INLINE_FRAGMENT';

typeSystemDirectiveLocation
    : 'SCHEMA'
    | 'SCALAR'
    | 'OBJECT'
    | 'FIELD_DEFINITION'
    | 'ARGUMENT_DEFINITION'
    | 'INTERFACE'
    | 'UNION'
    | 'ENUM'
    | 'ENUM_VALUE'
    | 'INPUT_OBJECT'
    | 'INPUT_FIELD_DEFINITION';