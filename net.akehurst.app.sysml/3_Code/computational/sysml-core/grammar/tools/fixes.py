# manual corrections applied after conversion of the OMG BNF
FIXES = {
    # visibility of an import is optional (as in the pilot implementation and the 2023-11 grammar)
    "    Import = VisibilityIndicator 'import'": "    Import = VisibilityIndicator ? 'import'",
    # 'assert' is optional, as in the pilot implementation
    "    SatisfyRequirementUsage = OccurrenceUsagePrefix 'assert' ( 'not' ) 'satisfy'": "    SatisfyRequirementUsage = OccurrenceUsagePrefix ( 'assert' ) ? ( 'not' ) ? 'satisfy'",
}

# whole rules replaced by hand (agl syntax)
OVERRIDES = {
    # 'standard' is optional
    "LibraryPackage": "( 'standard' ) ? 'library' PrefixMetadataMember * PackageDeclaration PackageBody",
    # the subsetting part of a flow end is followed by a '.' (as in KerML)
    "FlowEndSubsetting": "QualifiedName '.' | FeatureChainPrefix",
    # end features of connection definitions: 'end item x', 'end port p', 'end x'
    "OccurrenceUsagePrefix": "( EndUsagePrefix | BasicUsagePrefix ) ( 'individual' ) ? ( PortionKind ) ? UsageExtensionKeyword *",
    "DefaultReferenceUsage": "( EndUsagePrefix | RefPrefix ) Usage",
    # the source end of TargetSuccession is empty, so 'then' must not be repeated
    "EntryTransitionMember": "MemberPrefix ( GuardedTargetSuccession | TargetSuccession ) ';'",
    # a send node is declared with 'action' (as for the other action nodes)
    "SendNode": "OccurrenceUsagePrefix ActionNodeUsageDeclaration ? 'send' ( NodeParameterMember SenderReceiverPart ? | SenderReceiverPart ) ? ActionBody",
    # enumerated values may have prefix metadata
    "EnumeratedValue": "UsageExtensionKeyword * 'enum' ? Usage",
    # cases may declare a return parameter, as calculations can
    "CaseBodyItem": "ActionBodyItem | SubjectMember | ActorMember | ObjectiveMember | ReturnParameterMember",
    # expressions: the 2.0 BNF gives one ambiguous operator rule, use the precedence layers of KerML 8.2.5.8.1 instead
    "OwnedExpression": "ConditionalExpression",
    "ConditionalExpression": "NullCoalescingExpression | 'if' NullCoalescingExpression '?' OwnedExpression 'else' OwnedExpression",
    "NullCoalescingExpression": "ImpliesExpression ( '??' ImpliesExpression ) *",
    "ImpliesExpression": "OrExpression ( 'implies' OrExpression ) *",
    "OrExpression": "XorExpression ( OrOperator XorExpression ) *",
    "OrOperator": "'|' | 'or'",
    "XorExpression": "AndExpression ( 'xor' AndExpression ) *",
    "AndExpression": "EqualityExpression ( AndOperator EqualityExpression ) *",
    "AndOperator": "'&' | 'and'",
    "EqualityExpression": "ClassificationExpression ( EqualityOperator ClassificationExpression ) *",
    "EqualityOperator": "'==' | '!=' | '===' | '!=='",
    "ClassificationExpression": "RelationalExpression ( ClassificationTestOperator TypeReference | CastOperator TypeReference ) ? | ClassificationTestOperator TypeReference | CastOperator TypeReference | MetadataReference MetaclassificationTestOperator TypeReference | MetadataReference MetaCastOperator TypeReference",
    "MetadataReference": "QualifiedName",
    "RelationalExpression": "RangeExpression ( RelationalOperator RangeExpression ) *",
    "RelationalOperator": "'<' | '>' | '<=' | '>='",
    "RangeExpression": "AdditiveExpression ( '..' AdditiveExpression ) ?",
    "AdditiveExpression": "MultiplicativeExpression ( AdditiveOperator MultiplicativeExpression ) *",
    "AdditiveOperator": "'+' | '-'",
    "MultiplicativeExpression": "ExponentiationExpression ( MultiplicativeOperator ExponentiationExpression ) *",
    "MultiplicativeOperator": "'*' | '/' | '%'",
    "ExponentiationExpression": "UnaryExpression ( ExponentiationOperator ExponentiationExpression ) ?",
    "ExponentiationOperator": "'**' | '^'",
    "UnaryExpression": "UnaryOperator ExtentExpression | ExtentExpression",
    "ExtentExpression": "'all' TypeReference | PrimaryExpression",
    "PrimaryExpression": "BaseExpression PrimaryExpressionPart *",
    "PrimaryExpressionPart": "'.' QualifiedName | '#' '(' SequenceExpressionList ')' | '[' SequenceExpressionList ']' | '->' InstantiatedTypeMember ( BodyExpression | FunctionReferenceExpression | ArgumentList ) | '.' BodyExpression | '.?' BodyExpression",
    "BaseExpression": "NullExpression | LiteralExpression | FeatureReferenceExpression | MetadataAccessExpression | InvocationExpression | ConstructorExpression | BodyExpression | SequenceExpression",
    "SequenceExpression": "'(' SequenceExpressionList ')'",
    "SequenceExpressionList": "OwnedExpression ( ',' OwnedExpression ) * ( ',' ) ?",
}

# alternatives added to a converted rule
EXTEND = {
    # allocation definitions are missing from the definition elements
    "DefinitionElement": "AllocationDefinition",
}
