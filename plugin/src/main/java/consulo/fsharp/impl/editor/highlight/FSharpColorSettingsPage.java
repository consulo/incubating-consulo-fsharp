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

package consulo.fsharp.impl.editor.highlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.fsharp.FSharpLanguage;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.localize.LocalizeValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class FSharpColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = {
        new AttributesDescriptor("Keywords//Keyword", FSharpHighlightKey.KEYWORD),
        new AttributesDescriptor("Keywords//Preprocessor keyword", FSharpHighlightKey.PREPROCESSOR_KEYWORD),
        new AttributesDescriptor("Keywords//Predefined type (int, string, unit)", FSharpHighlightKey.PREDEFINED_TYPE),
        new AttributesDescriptor("Comments//Line comment", FSharpHighlightKey.LINE_COMMENT),
        new AttributesDescriptor("Comments//Block comment", FSharpHighlightKey.BLOCK_COMMENT),
        new AttributesDescriptor("String", FSharpHighlightKey.STRING),
        new AttributesDescriptor("Number", FSharpHighlightKey.NUMBER),
        new AttributesDescriptor("Braces and Operators//Parentheses", FSharpHighlightKey.PARENTHESES),
        new AttributesDescriptor("Braces and Operators//Brackets", FSharpHighlightKey.BRACKETS),
        new AttributesDescriptor("Braces and Operators//Braces", FSharpHighlightKey.BRACES),
        new AttributesDescriptor("Braces and Operators//Attribute brackets", FSharpHighlightKey.ATTRIBUTE_BRACKETS),
        new AttributesDescriptor("Braces and Operators//Dot", FSharpHighlightKey.DOT),
        new AttributesDescriptor("Braces and Operators//Comma", FSharpHighlightKey.COMMA),
        new AttributesDescriptor("Braces and Operators//Semicolon", FSharpHighlightKey.SEMICOLON),
        new AttributesDescriptor("Braces and Operators//Operator sign", FSharpHighlightKey.OPERATION_SIGN),
        new AttributesDescriptor("Types//Class", FSharpHighlightKey.CLASS_NAME),
        new AttributesDescriptor("Types//Interface", FSharpHighlightKey.INTERFACE_NAME),
        new AttributesDescriptor("Types//Struct", FSharpHighlightKey.STRUCT_NAME),
        new AttributesDescriptor("Types//Enum", FSharpHighlightKey.ENUM_NAME),
        new AttributesDescriptor("Types//Record", FSharpHighlightKey.RECORD_NAME),
        new AttributesDescriptor("Types//Union", FSharpHighlightKey.UNION_NAME),
        new AttributesDescriptor("Types//Module", FSharpHighlightKey.MODULE_NAME),
        new AttributesDescriptor("Types//Type parameter", FSharpHighlightKey.TYPE_PARAMETER),
        new AttributesDescriptor("Union case and enum case", FSharpHighlightKey.UNION_CASE),
        new AttributesDescriptor("Constant", FSharpHighlightKey.CONSTANT),
        new AttributesDescriptor("Functions and Members//Function", FSharpHighlightKey.FUNCTION),
        new AttributesDescriptor("Functions and Members//Module value", FSharpHighlightKey.VALUE),
        new AttributesDescriptor("Functions and Members//Method", FSharpHighlightKey.METHOD),
        new AttributesDescriptor("Functions and Members//Static method", FSharpHighlightKey.STATIC_METHOD),
        new AttributesDescriptor("Functions and Members//Field", FSharpHighlightKey.FIELD),
        new AttributesDescriptor("Functions and Members//Property", FSharpHighlightKey.PROPERTY),
        new AttributesDescriptor("Functions and Members//Static field", FSharpHighlightKey.STATIC_FIELD),
        new AttributesDescriptor("Variables//Parameter", FSharpHighlightKey.PARAMETER),
        new AttributesDescriptor("Variables//Local variable", FSharpHighlightKey.LOCAL_VARIABLE),
    };

    private static final Map<String, TextAttributesKey> TAGS = new LinkedHashMap<>();

    static {
        TAGS.put("predefined", FSharpHighlightKey.PREDEFINED_TYPE);
        TAGS.put("class", FSharpHighlightKey.CLASS_NAME);
        TAGS.put("interface", FSharpHighlightKey.INTERFACE_NAME);
        TAGS.put("struct", FSharpHighlightKey.STRUCT_NAME);
        TAGS.put("enum", FSharpHighlightKey.ENUM_NAME);
        TAGS.put("record", FSharpHighlightKey.RECORD_NAME);
        TAGS.put("union", FSharpHighlightKey.UNION_NAME);
        TAGS.put("module", FSharpHighlightKey.MODULE_NAME);
        TAGS.put("case", FSharpHighlightKey.UNION_CASE);
        TAGS.put("constant", FSharpHighlightKey.CONSTANT);
        TAGS.put("function", FSharpHighlightKey.FUNCTION);
        TAGS.put("value", FSharpHighlightKey.VALUE);
        TAGS.put("method", FSharpHighlightKey.METHOD);
        TAGS.put("staticMethod", FSharpHighlightKey.STATIC_METHOD);
        TAGS.put("field", FSharpHighlightKey.FIELD);
        TAGS.put("property", FSharpHighlightKey.PROPERTY);
        TAGS.put("parameter", FSharpHighlightKey.PARAMETER);
        TAGS.put("local", FSharpHighlightKey.LOCAL_VARIABLE);
    }

    private static final String DEMO_TEXT = """
        namespace Demo

        open System

        /// A shape
        type <union>Shape</union> =
            | <case>Circle</case> of radius: <predefined>float</predefined>
            | <case>Square</case> of side: <predefined>float</predefined>

        type <record>Measurement</record> = { <field>Name</field>: <predefined>string</predefined>; <field>Area</field>: <predefined>float</predefined> }

        type <interface>IShape</interface> =
            abstract <method>Area</method>: <predefined>unit</predefined> -> <predefined>float</predefined>

        type <class>Box</class>(<parameter>size</parameter>: <predefined>float</predefined>) =
            member <parameter>_</parameter>.<method>Size</method> = <parameter>size</parameter>
            member val <property>Label</property> = "box" with get, set
            interface <interface>IShape</interface> with
                member <parameter>_</parameter>.<method>Area</method>() = <parameter>size</parameter> * <parameter>size</parameter>

        module <module>ShapeOps</module> =
            let <value>unit</value> = 1.0 (* block comment *)

            let <function>area</function> <parameter>shape</parameter> =
                match <parameter>shape</parameter> with
                | <case>Circle</case> <local>r</local> -> <class>Math</class>.<constant>PI</constant> * <local>r</local> * <local>r</local>
                | <case>Square</case> <local>s</local> -> <local>s</local> * <local>s</local>

            let <function>describe</function> (<parameter>shape</parameter>: <union>Shape</union>) =
                <class>String</class>.<staticMethod>Format</staticMethod>("{0}: {1}", <parameter>shape</parameter>, <function>area</function> <parameter>shape</parameter>)

        #if DEBUG
        let <value>debug</value> = [ 1; 2; 3 ] |> <module>List</module>.<function>map</function> (fun <parameter>x</parameter> -> <parameter>x</parameter> + 1)
        #endif
        """;

    @Override
    public SyntaxHighlighter getHighlighter() {
        return new FSharpSyntaxHighlighter();
    }

    @Override
    public String getDemoText() {
        return DEMO_TEXT;
    }

    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return TAGS;
    }

    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return FSharpLanguage.INSTANCE.getDisplayName();
    }
}
