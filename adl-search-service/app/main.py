from typing import List, Optional, Any, Dict
import re
import json
import yaml
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from pydantic import BaseModel

app = FastAPI(title="ADL Validator API")


class SyntaxErrorDetail(BaseModel):
    line: Optional[int]
    message: str


class ValidationResult(BaseModel):
    syntax_errors: List[SyntaxErrorDetail]
    used_tools: List[str]
    references: List[str]
    language: Optional[str]


def try_parse_json(text: str) -> Any:
    return json.loads(text)


def try_parse_yaml(text: str) -> Any:
    return yaml.safe_load(text)


def find_used_tools(text: str) -> List[str]:
    tools = set()
    # common patterns: tools:, uses:, run_tool(...), call_tool(...), @toolName
    for m in re.finditer(r"\b(?:tools|uses|use|tool)\b[:=]?\s*([A-Za-z0-9_.-]+)", text, re.IGNORECASE):
        tools.add(m.group(1))
    for m in re.finditer(r"([A-Za-z_][A-Za-z0-9_]*)\s*\(", text):
        name = m.group(1)
        if name.lower().startswith(("run_","call_","invoke_","tool_")):
            tools.add(name)
    for m in re.finditer(r"@([A-Za-z_][A-Za-z0-9_-]*)", text):
        tools.add(m.group(1))
    return sorted(tools)


def find_references(text: str) -> List[str]:
    refs = set()
    # URLs
    for m in re.finditer(r"https?://[^\s'\"<>]+", text):
        refs.add(m.group(0))
    # file paths (simple heuristic)
    for m in re.finditer(r"(?:[A-Za-z]:)?[\\/][\w\-./\\]+\.[a-zA-Z0-9]+", text):
        refs.add(m.group(0))
    # explicit ref: tokens
    for m in re.finditer(r"\bref(?:erence)?s?\b[:=]?\s*([A-Za-z0-9_./:-]+)", text, re.IGNORECASE):
        refs.add(m.group(1))
    return sorted(refs)


def find_syntax_issues_text(text: str) -> List[SyntaxErrorDetail]:
    errors: List[SyntaxErrorDetail] = []
    # unbalanced braces/brackets/parens
    pairs = {'(': ')', '{': '}', '[': ']'}
    stack: List[Dict[str, Any]] = []
    for i, ch in enumerate(text):
        if ch in pairs:
            stack.append({'char': ch, 'pos': i})
        elif ch in pairs.values():
            if not stack:
                errors.append(SyntaxErrorDetail(line=None, message=f"Unmatched closing '{ch}' at pos {i}"))
            else:
                last = stack.pop()
                if pairs[last['char']] != ch:
                    errors.append(SyntaxErrorDetail(line=None, message=f"Mismatched '{last['char']}' with '{ch}' at pos {i}"))
    for leftover in stack:
        errors.append(SyntaxErrorDetail(line=None, message=f"Unclosed '{leftover['char']}' starting at pos {leftover['pos']}"))

    # unclosed quotes
    for quote in ['"', "'"]:
        if text.count(quote) % 2 != 0:
            errors.append(SyntaxErrorDetail(line=None, message=f"Unclosed quote {quote}"))

    # indentation mix (tabs vs spaces)
    has_tabs = any(line.startswith('\t') for line in text.splitlines())
    has_spaces_indented = any(re.match(r" {2,}", line) for line in text.splitlines())
    if has_tabs and has_spaces_indented:
        errors.append(SyntaxErrorDetail(line=None, message="Mixed tabs and spaces for indentation"))

    return errors


@app.post('/validate', response_model=ValidationResult)
async def validate_adl(file: UploadFile = File(None), text: str = Form(None)) -> ValidationResult:
    if file is None and (text is None or text.strip() == ''):
        raise HTTPException(status_code=400, detail="Provide an ADL file upload or `text` form field")

    if file is not None:
        content_bytes = await file.read()
        try:
            content = content_bytes.decode('utf-8')
        except Exception:
            content = content_bytes.decode('latin-1')
    else:
        content = text

    syntax_errors: List[SyntaxErrorDetail] = []
    language: Optional[str] = None

    # try JSON
    try:
        _ = try_parse_json(content)
        language = 'json'
    except Exception as e_json:
        # try YAML
        try:
            _ = try_parse_yaml(content)
            language = 'yaml'
        except Exception as e_yaml:
            language = 'adl-text'
            # best-effort syntax heuristics
            syntax_errors = find_syntax_issues_text(content)

    used_tools = find_used_tools(content)
    references = find_references(content)

    return ValidationResult(
        syntax_errors=syntax_errors,
        used_tools=used_tools,
        references=references,
        language=language,
    )


if __name__ == '__main__':
    import uvicorn

    uvicorn.run('main:app', host='127.0.0.1', port=8000, reload=True)
