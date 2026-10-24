/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.fsharp.impl.lang.psi;

import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.impl.FSharpNamedElementImpl;
import consulo.fsharp.impl.lang.psi.impl.FSharpReferenceExpressionImpl;
import consulo.fsharp.impl.lang.psi.impl.FSharpReferenceNameImpl;
import consulo.fsharp.impl.lang.psi.impl.FSharpReferencePatImpl;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;

/**
 * F# composite node types. Names follow the ReSharper F# PSI ({@code FSharp.psi} in JetBrains resharper-fsharp,
 * Apache 2.0), so its parser test data can be reused.
 *
 * @author VISTALL
 * @since 24-Nov-16.
 */
public interface FSharpElementTypes {
    IFileElementType FSHARP_IMPL_FILE = FSharpStubElements.IMPL_FILE;
    IFileElementType FSHARP_SIG_FILE = FSharpStubElements.SIG_FILE;

    IElementType NAMED_NAMESPACE_DECLARATION = FSharpStubElements.NAMED_NAMESPACE_DECLARATION;
    IElementType GLOBAL_NAMESPACE_DECLARATION = FSharpStubElements.GLOBAL_NAMESPACE_DECLARATION;
    IElementType NAMED_MODULE_DECLARATION = FSharpStubElements.NAMED_MODULE_DECLARATION;
    IElementType ANON_MODULE_DECLARATION = FSharpStubElements.ANON_MODULE_DECLARATION;
    IElementType LET_BINDINGS_DECLARATION = new FSharpCompositeElementType("LetBindingsDeclaration");
    IElementType BINDING_SIGNATURE = new FSharpCompositeElementType("BindingSignature");
    IElementType TOP_BINDING = new FSharpCompositeElementType("TopBinding");
    IElementType LOCAL_BINDING = new FSharpCompositeElementType("LocalBinding");
    IElementType RETURN_TYPE_INFO = new FSharpCompositeElementType("ReturnTypeInfo");
    IElementType EXTERN_DECLARATION = new FSharpCompositeElementType("ExternDeclaration");
    IElementType LOCAL_REFERENCE_PAT = new FSharpCompositeElementType("LocalReferencePat", FSharpReferencePatImpl::new);
    IElementType TOP_REFERENCE_PAT = new FSharpCompositeElementType("TopReferencePat", FSharpReferencePatImpl::new);
    IElementType PARAMETERS_OWNER_PAT = new FSharpCompositeElementType("ParametersOwnerPat");
    IElementType AS_PAT = new FSharpCompositeElementType("AsPat");
    IElementType TYPED_PAT = new FSharpCompositeElementType("TypedPat");
    IElementType ATTRIB_PAT = new FSharpCompositeElementType("AttribPat");
    IElementType OR_PAT = new FSharpCompositeElementType("OrPat");
    IElementType ANDS_PAT = new FSharpCompositeElementType("AndsPat");
    IElementType ARRAY_PAT = new FSharpCompositeElementType("ArrayPat");
    IElementType LIST_PAT = new FSharpCompositeElementType("ListPat");
    IElementType TUPLE_PAT = new FSharpCompositeElementType("TuplePat");
    IElementType PAREN_PAT = new FSharpCompositeElementType("ParenPat");
    IElementType RECORD_PAT = new FSharpCompositeElementType("RecordPat");
    IElementType NAMED_UNION_CASE_FIELDS_PAT = new FSharpCompositeElementType("NamedUnionCaseFieldsPat");
    IElementType FIELD_PAT = new FSharpCompositeElementType("FieldPat");
    IElementType OPTIONAL_VAL_PAT = new FSharpCompositeElementType("OptionalValPat");
    IElementType IS_INST_PAT = new FSharpCompositeElementType("IsInstPat");
    IElementType LIST_CONS_PAT = new FSharpCompositeElementType("ListConsPat");
    IElementType WILD_PAT = new FSharpCompositeElementType("WildPat");
    IElementType LITERAL_PAT = new FSharpCompositeElementType("LiteralPat");
    IElementType UNIT_PAT = new FSharpCompositeElementType("UnitPat");
    IElementType NULL_PAT = new FSharpCompositeElementType("NullPat");
    IElementType QUOTE_EXPR_PAT = new FSharpCompositeElementType("QuoteExprPat");
    IElementType CHAR_RANGE_PAT = new FSharpCompositeElementType("CharRangePat");
    IElementType FROM_ERROR_PAT = new FSharpCompositeElementType("FromErrorPat");
    IElementType SET_EXPR = new FSharpCompositeElementType("SetExpr");
    IElementType ITEM_INDEXER_EXPR = new FSharpCompositeElementType("ItemIndexerExpr");
    IElementType NAMED_INDEXER_EXPR = new FSharpCompositeElementType("NamedIndexerExpr");
    IElementType INDEXER_ARG_LIST = new FSharpCompositeElementType("IndexerArgList");
    IElementType PAREN_EXPR = new FSharpCompositeElementType("ParenExpr");
    IElementType BEGIN_END_EXPR = new FSharpCompositeElementType("BeginEndExpr");
    IElementType QUOTE_EXPR = new FSharpCompositeElementType("QuoteExpr");
    IElementType UNIT_EXPR = new FSharpCompositeElementType("UnitExpr");
    IElementType LITERAL_EXPR = new FSharpCompositeElementType("LiteralExpr");
    IElementType UNIT_OF_MEASURE_CLAUSE = new FSharpCompositeElementType("UnitOfMeasureClause");
    IElementType NAMED_MEASURE = new FSharpCompositeElementType("NamedMeasure");
    IElementType PRODUCT_MEASURE = new FSharpCompositeElementType("ProductMeasure");
    IElementType SEQ_MEASURE = new FSharpCompositeElementType("SeqMeasure");
    IElementType DIVIDE_MEASURE = new FSharpCompositeElementType("DivideMeasure");
    IElementType POWER_MEASURE = new FSharpCompositeElementType("PowerMeasure");
    IElementType ONE_MEASURE = new FSharpCompositeElementType("OneMeasure");
    IElementType ANON_MEASURE = new FSharpCompositeElementType("AnonMeasure");
    IElementType VAR_MEASURE = new FSharpCompositeElementType("VarMeasure");
    IElementType PAREN_MEASURE = new FSharpCompositeElementType("ParenMeasure");
    IElementType POWER_CLAUSE = new FSharpCompositeElementType("PowerClause");
    IElementType RATIONAL_CONST = new FSharpCompositeElementType("RationalConst");
    IElementType INTEGER_RAT = new FSharpCompositeElementType("IntegerRat");
    IElementType RATIONAL_RAT = new FSharpCompositeElementType("RationalRat");
    IElementType NEGATE_RAT = new FSharpCompositeElementType("NegateRat");
    IElementType PAREN_RAT = new FSharpCompositeElementType("ParenRat");
    IElementType TYPED_EXPR = new FSharpCompositeElementType("TypedExpr");
    IElementType TUPLE_EXPR = new FSharpCompositeElementType("TupleExpr");
    IElementType ANON_RECORD_EXPR = new FSharpCompositeElementType("AnonRecordExpr");
    IElementType ARRAY_EXPR = new FSharpCompositeElementType("ArrayExpr");
    IElementType LIST_EXPR = new FSharpCompositeElementType("ListExpr");
    IElementType RECORD_EXPR = new FSharpCompositeElementType("RecordExpr");
    IElementType RECORD_MEMBER_BINDING_LIST = new FSharpCompositeElementType("RecordMemberBindingList");
    IElementType RECORD_FIELD_BINDING = new FSharpCompositeElementType("RecordFieldBinding");
    IElementType EXPR_TYPE_SPREAD = new FSharpCompositeElementType("ExprTypeSpread");
    IElementType NEW_EXPR = new FSharpCompositeElementType("NewExpr");
    IElementType FOR_EXPR = new FSharpCompositeElementType("ForExpr");
    IElementType FOR_EACH_EXPR = new FSharpCompositeElementType("ForEachExpr");
    IElementType WHILE_EXPR = new FSharpCompositeElementType("WhileExpr");
    IElementType OBJ_EXPR = new FSharpCompositeElementType("ObjExpr");
    IElementType COMPUTATION_EXPR = new FSharpCompositeElementType("ComputationExpr");
    IElementType LAMBDA_EXPR = new FSharpCompositeElementType("LambdaExpr");
    IElementType DOT_LAMBDA_EXPR = new FSharpCompositeElementType("DotLambdaExpr");
    IElementType LAMBDA_PARAMETERS_LIST = new FSharpCompositeElementType("LambdaParametersList");
    IElementType MATCH_LAMBDA_EXPR = new FSharpCompositeElementType("MatchLambdaExpr");
    IElementType MATCH_EXPR = new FSharpCompositeElementType("MatchExpr");
    IElementType DO_EXPR = new FSharpCompositeElementType("DoExpr");
    IElementType ASSERT_EXPR = new FSharpCompositeElementType("AssertExpr");
    IElementType PREFIX_APP_EXPR = new FSharpCompositeElementType("PrefixAppExpr");
    IElementType BINARY_APP_EXPR = new FSharpCompositeElementType("BinaryAppExpr");
    IElementType DYNAMIC_EXPR = new FSharpCompositeElementType("DynamicExpr");
    IElementType LET_OR_USE_EXPR = new FSharpCompositeElementType("LetOrUseExpr");
    IElementType TRY_WITH_EXPR = new FSharpCompositeElementType("TryWithExpr");
    IElementType TRY_FINALLY_EXPR = new FSharpCompositeElementType("TryFinallyExpr");
    IElementType LAZY_EXPR = new FSharpCompositeElementType("LazyExpr");
    IElementType SEQUENTIAL_EXPR = new FSharpCompositeElementType("SequentialExpr");
    IElementType IF_THEN_ELSE_EXPR = new FSharpCompositeElementType("IfThenElseExpr");
    IElementType ELIF_EXPR = new FSharpCompositeElementType("ElifExpr");
    IElementType REFERENCE_EXPR = new FSharpCompositeElementType("ReferenceExpr", FSharpReferenceExpressionImpl::new);
    IElementType MATCH_CLAUSE = new FSharpCompositeElementType("MatchClause");
    IElementType WHEN_EXPR_CLAUSE = new FSharpCompositeElementType("WhenExprClause");
    IElementType TYPE_TEST_EXPR = new FSharpCompositeElementType("TypeTestExpr");
    IElementType UPCAST_EXPR = new FSharpCompositeElementType("UpcastExpr");
    IElementType DOWNCAST_EXPR = new FSharpCompositeElementType("DowncastExpr");
    IElementType INFERRED_UPCAST_EXPR = new FSharpCompositeElementType("InferredUpcastExpr");
    IElementType INFERRED_DOWNCAST_EXPR = new FSharpCompositeElementType("InferredDowncastExpr");
    IElementType NULL_EXPR = new FSharpCompositeElementType("NullExpr");
    IElementType ADDRESS_OF_EXPR = new FSharpCompositeElementType("AddressOfExpr");
    IElementType TRAIT_CALL_EXPR = new FSharpCompositeElementType("TraitCallExpr");
    IElementType JOIN_IN_EXPR = new FSharpCompositeElementType("JoinInExpr");
    IElementType IMPLICIT_ZERO_EXPR = new FSharpCompositeElementType("ImplicitZeroExpr");
    IElementType YIELD_OR_RETURN_EXPR = new FSharpCompositeElementType("YieldOrReturnExpr");
    IElementType LIBRARY_ONLY_EXPR = new FSharpCompositeElementType("LibraryOnlyExpr");
    IElementType FROM_ERROR_EXPR = new FSharpCompositeElementType("FromErrorExpr");
    IElementType INTERPOLATED_STRING_EXPR = new FSharpCompositeElementType("InterpolatedStringExpr");
    IElementType FIXED_EXPR = new FSharpCompositeElementType("FixedExpr");
    IElementType RANGE_EXPR = new FSharpCompositeElementType("RangeExpr");
    IElementType BEGINNING_SLICE_EXPR = new FSharpCompositeElementType("BeginningSliceExpr");
    IElementType END_SLICE_EXPR = new FSharpCompositeElementType("EndSliceExpr");
    IElementType WHOLE_RANGE_EXPR = new FSharpCompositeElementType("WholeRangeExpr");
    IElementType CHAMELEON_EXPRESSION = new FSharpCompositeElementType("ChameleonExpression");
    IElementType TYPE_DECLARATION_GROUP = new FSharpCompositeElementType("TypeDeclarationGroup");
    IElementType FSHARP_TYPE_DECLARATION = FSharpStubElements.FSHARP_TYPE_DECLARATION;
    IElementType CLASS_REPRESENTATION = new FSharpCompositeElementType("ClassRepresentation");
    IElementType STRUCT_REPRESENTATION = new FSharpCompositeElementType("StructRepresentation");
    IElementType INTERFACE_REPRESENTATION = new FSharpCompositeElementType("InterfaceRepresentation");
    IElementType NESTED_MODULE_DECLARATION = FSharpStubElements.NESTED_MODULE_DECLARATION;
    IElementType TYPE_EXTENSION_DECLARATION = new FSharpCompositeElementType("TypeExtensionDeclaration");
    IElementType ENUM_REPRESENTATION = new FSharpCompositeElementType("EnumRepresentation");
    IElementType ENUM_CASE_DECLARATION = new FSharpCompositeElementType("EnumCaseDeclaration", FSharpNamedElementImpl::new);
    IElementType RECORD_REPRESENTATION = new FSharpCompositeElementType("RecordRepresentation");
    IElementType RECORD_MEMBER_DECLARATION_LIST = new FSharpCompositeElementType("RecordMemberDeclarationList");
    IElementType RECORD_FIELD_DECLARATION = new FSharpCompositeElementType("RecordFieldDeclaration", FSharpNamedElementImpl::new);
    IElementType DECLARATION_TYPE_SPREAD = new FSharpCompositeElementType("DeclarationTypeSpread");
    IElementType UNION_REPRESENTATION = new FSharpCompositeElementType("UnionRepresentation");
    IElementType UNION_CASE_DECLARATION = new FSharpCompositeElementType("UnionCaseDeclaration", FSharpNamedElementImpl::new);
    IElementType UNION_CASE_FIELD_DECLARATION_LIST = new FSharpCompositeElementType("UnionCaseFieldDeclarationList");
    IElementType UNION_CASE_FIELD_DECLARATION = new FSharpCompositeElementType("UnionCaseFieldDeclaration");
    IElementType EXCEPTION_FIELD_DECLARATION = new FSharpCompositeElementType("ExceptionFieldDeclaration");
    IElementType EXCEPTION_DECLARATION = FSharpStubElements.EXCEPTION_DECLARATION;
    IElementType TYPE_ABBREVIATION_REPRESENTATION = new FSharpCompositeElementType("TypeAbbreviationRepresentation");
    IElementType TYPE_USAGE_OR_UNION_CASE_DECLARATION = new FSharpCompositeElementType("TypeUsageOrUnionCaseDeclaration");
    IElementType IL_ASSEMBLY_REPRESENTATION = new FSharpCompositeElementType("IlAssemblyRepresentation");
    IElementType MODULE_ABBREVIATION_DECLARATION = new FSharpCompositeElementType("ModuleAbbreviationDeclaration", FSharpNamedElementImpl::new);
    IElementType ACTIVE_PATTERN_NAMED_CASE_REFERENCE_NAME = new FSharpCompositeElementType("ActivePatternNamedCaseReferenceName");
    IElementType TOP_ACTIVE_PATTERN_CASE_DECLARATION = new FSharpCompositeElementType("TopActivePatternCaseDeclaration", FSharpNamedElementImpl::new);
    IElementType LOCAL_ACTIVE_PATTERN_CASE_DECLARATION = new FSharpCompositeElementType("LocalActivePatternCaseDeclaration", FSharpNamedElementImpl::new);
    IElementType WILD_ACTIVE_PATTERN_WILD_CASE = new FSharpCompositeElementType("WildActivePatternWildCase");
    IElementType DELEGATE_REPRESENTATION = new FSharpCompositeElementType("DelegateRepresentation");
    IElementType INTERFACE_INHERIT = new FSharpCompositeElementType("InterfaceInherit");
    IElementType TYPE_INHERIT = new FSharpCompositeElementType("TypeInherit");
    IElementType INTERFACE_IMPLEMENTATION = new FSharpCompositeElementType("InterfaceImplementation");
    IElementType PRIMARY_CONSTRUCTOR_DECLARATION = new FSharpCompositeElementType("PrimaryConstructorDeclaration");
    IElementType CTOR_SELF_ID = new FSharpCompositeElementType("CtorSelfId", FSharpNamedElementImpl::new);
    IElementType NAMED_SELF_ID = new FSharpCompositeElementType("NamedSelfId", FSharpNamedElementImpl::new);
    IElementType WILD_SELF_ID = new FSharpCompositeElementType("WildSelfId");
    IElementType CONSTRUCTOR_SIGNATURE = new FSharpCompositeElementType("ConstructorSignature");
    IElementType SECONDARY_CONSTRUCTOR_DECLARATION = new FSharpCompositeElementType("SecondaryConstructorDeclaration");
    IElementType MEMBER_DECLARATION = new FSharpCompositeElementType("MemberDeclaration", FSharpNamedElementImpl::new);
    IElementType MEMBER_SIGNATURE = new FSharpCompositeElementType("MemberSignature", FSharpNamedElementImpl::new);
    IElementType ABSTRACT_MEMBER_DECLARATION = new FSharpCompositeElementType("AbstractMemberDeclaration", FSharpNamedElementImpl::new);
    IElementType VAL_FIELD_DECLARATION = new FSharpCompositeElementType("ValFieldDeclaration", FSharpNamedElementImpl::new);
    IElementType AUTO_PROPERTY_DECLARATION = new FSharpCompositeElementType("AutoPropertyDeclaration", FSharpNamedElementImpl::new);
    IElementType ACCESSOR_DECLARATION = new FSharpCompositeElementType("AccessorDeclaration");
    IElementType PARAMETERS_PATTERN_DECLARATION = new FSharpCompositeElementType("ParametersPatternDeclaration");
    IElementType LINE_DIRECTIVE = new FSharpCompositeElementType("LineDirective");
    IElementType LOAD_DIRECTIVE = new FSharpCompositeElementType("LoadDirective");
    IElementType REFERENCE_DIRECTIVE = new FSharpCompositeElementType("ReferenceDirective");
    IElementType I_DIRECTIVE = new FSharpCompositeElementType("IDirective");
    IElementType WARNING_DIRECTIVE = new FSharpCompositeElementType("WarningDirective");
    IElementType OTHER_DIRECTIVE = new FSharpCompositeElementType("OtherDirective");
    IElementType OPEN_STATEMENT = new FSharpCompositeElementType("OpenStatement");
    IElementType DO_STATEMENT = new FSharpCompositeElementType("DoStatement");
    IElementType EXPRESSION_STATEMENT = new FSharpCompositeElementType("ExpressionStatement");
    IElementType ACTIVE_PATTERN_ID = new FSharpCompositeElementType("ActivePatternId");
    IElementType DOT_LAMBDA_ID = new FSharpCompositeElementType("DotLambdaId");
    IElementType TYPE_PARAMETER_ID = new FSharpCompositeElementType("TypeParameterId");
    IElementType POSTFIX_TYPE_PARAMETER_DECLARATION_LIST = new FSharpCompositeElementType("PostfixTypeParameterDeclarationList");
    IElementType PREFIX_TYPE_PARAMETER_DECLARATION_LIST = new FSharpCompositeElementType("PrefixTypeParameterDeclarationList");
    IElementType TYPE_PARAMETER_OF_TYPE_DECLARATION = new FSharpCompositeElementType("TypeParameterOfTypeDeclaration");
    IElementType TYPE_PARAMETER_OF_METHOD_DECLARATION = new FSharpCompositeElementType("TypeParameterOfMethodDeclaration");
    IElementType TYPE_CONSTRAINTS_CLAUSE = new FSharpCompositeElementType("TypeConstraintsClause");
    IElementType VALUE_TYPE_CONSTRAINT = new FSharpCompositeElementType("ValueTypeConstraint");
    IElementType REFERENCE_TYPE_CONSTRAINT = new FSharpCompositeElementType("ReferenceTypeConstraint");
    IElementType UNMANAGED_TYPE_CONSTRAINT = new FSharpCompositeElementType("UnmanagedTypeConstraint");
    IElementType NULL_CONSTRAINT = new FSharpCompositeElementType("NullConstraint");
    IElementType NOT_NULL_CONSTRAINT = new FSharpCompositeElementType("NotNullConstraint");
    IElementType COMPARABLE_CONSTRAINT = new FSharpCompositeElementType("ComparableConstraint");
    IElementType EQUATABLE_CONSTRAINT = new FSharpCompositeElementType("EquatableConstraint");
    IElementType DEFAULTS_TO_CONSTRAINT = new FSharpCompositeElementType("DefaultsToConstraint");
    IElementType SUBTYPE_CONSTRAINT = new FSharpCompositeElementType("SubtypeConstraint");
    IElementType SELF_CONSTRAINT = new FSharpCompositeElementType("SelfConstraint");
    IElementType MEMBER_CONSTRAINT = new FSharpCompositeElementType("MemberConstraint");
    IElementType ENUM_CONSTRAINT = new FSharpCompositeElementType("EnumConstraint");
    IElementType DELEGATE_CONSTRAINT = new FSharpCompositeElementType("DelegateConstraint");
    IElementType ATTRIBUTE_LIST = new FSharpCompositeElementType("AttributeList");
    IElementType ATTRIBUTE = new FSharpCompositeElementType("Attribute");
    IElementType ATTRIBUTE_TARGET = new FSharpCompositeElementType("AttributeTarget");
    IElementType LOCAL_DECLARATION = new FSharpCompositeElementType("LocalDeclaration", FSharpNamedElementImpl::new);
    IElementType POSTFIX_APP_TYPE_ARGUMENT_LIST = new FSharpCompositeElementType("PostfixAppTypeArgumentList");
    IElementType PREFIX_APP_TYPE_ARGUMENT_LIST = new FSharpCompositeElementType("PrefixAppTypeArgumentList");
    IElementType EXPRESSION_REFERENCE_NAME = new FSharpCompositeElementType("ExpressionReferenceName", FSharpReferenceNameImpl::new);
    IElementType TYPE_REFERENCE_NAME = new FSharpCompositeElementType("TypeReferenceName", FSharpReferenceNameImpl::new);
    IElementType NAMED_TYPE_USAGE = new FSharpCompositeElementType("NamedTypeUsage");
    IElementType TUPLE_TYPE_USAGE = new FSharpCompositeElementType("TupleTypeUsage");
    IElementType ANON_RECORD_TYPE_USAGE = new FSharpCompositeElementType("AnonRecordTypeUsage");
    IElementType ANON_RECORD_FIELD = new FSharpCompositeElementType("AnonRecordField");
    IElementType ARRAY_TYPE_USAGE = new FSharpCompositeElementType("ArrayTypeUsage");
    IElementType FUNCTION_TYPE_USAGE = new FSharpCompositeElementType("FunctionTypeUsage");
    IElementType ANON_TYPE_USAGE = new FSharpCompositeElementType("AnonTypeUsage");
    IElementType PAREN_TYPE_USAGE = new FSharpCompositeElementType("ParenTypeUsage");
    IElementType PARAMETER_SIGNATURE_TYPE_USAGE = new FSharpCompositeElementType("ParameterSignatureTypeUsage");
    IElementType EXPR_STATIC_CONSTANT_TYPE_USAGE = new FSharpCompositeElementType("ExprStaticConstantTypeUsage");
    IElementType NAMED_STATIC_CONSTANT_TYPE_USAGE = new FSharpCompositeElementType("NamedStaticConstantTypeUsage");
    IElementType CONSTRAINED_TYPE_USAGE = new FSharpCompositeElementType("ConstrainedTypeUsage");
    IElementType OR_TYPE_USAGE = new FSharpCompositeElementType("OrTypeUsage");
    IElementType HASH_TYPE_USAGE = new FSharpCompositeElementType("HashTypeUsage");
    IElementType INTERSECTION_TYPE_USAGE = new FSharpCompositeElementType("IntersectionTypeUsage");
    IElementType WITH_NULL_TYPE_USAGE = new FSharpCompositeElementType("WithNullTypeUsage");
    IElementType UNSUPPORTED_TYPE_USAGE = new FSharpCompositeElementType("UnsupportedTypeUsage");
}
