define([], function() {
	var keywords = "as|case|catch|cd|cmd|compile|config|default|depends|deployment|do|else|eval|extends|extension|false|finally|for|if|import|includes|instanceof|new|null|on|package|param|return|run|scp|static|subsystem|super|switch|synchronized|throw|to|true|try|typeof|val|var|while";
	return {
		id: "xtext.amelia",
		contentTypes: ["xtext/amelia"],
		patterns: [
			{include: "orion.c-like#comment_singleLine"},
			{include: "orion.c-like#comment_block"},
			{include: "orion.lib#string_doubleQuote"},
			{include: "orion.lib#string_singleQuote"},
			{include: "orion.lib#number_decimal"},
			{include: "orion.lib#number_hex"},
			{include: "orion.lib#brace_open"},
			{include: "orion.lib#brace_close"},
			{include: "orion.lib#bracket_open"},
			{include: "orion.lib#bracket_close"},
			{include: "orion.lib#parenthesis_open"},
			{include: "orion.lib#parenthesis_close"},
			{name: "keyword.amelia", match: "\\b(?:" + keywords + ")\\b"}
		]
	};
});
