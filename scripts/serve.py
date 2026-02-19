from http.server import SimpleHTTPRequestHandler
from socketserver import TCPServer

class Handler(SimpleHTTPRequestHandler):
    extensions_map = SimpleHTTPRequestHandler.extensions_map.copy()
    extensions_map.update({
        ".java": "text/plain; charset=utf-8",
        ".gradle": "text/plain; charset=utf-8",
        ".json": "text/plain; charset=utf-8",
        ".xml": "text/plain; charset=utf-8",
        ".properties": "text/plain; charset=utf-8",
        ".yml": "text/plain; charset=utf-8",
        ".yaml": "text/plain; charset=utf-8",
        ".md": "text/plain; charset=utf-8",
    })

TCPServer(("127.0.0.1", 8000), Handler).serve_forever()
