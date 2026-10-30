function backendBase(): string {
  const raw = process.env.BACKEND_URL || "http://localhost:8080";
  return raw.endsWith("/") ? raw.slice(0, -1) : raw;
}

export async function proxyRequest(request: Request, pathname: string): Promise<Response> {
  const incoming = new URL(request.url);
  const target = `${backendBase()}${pathname}${incoming.search}`;
  const headers = new Headers();
  const userId = request.headers.get("x-user-id");
  if (userId) headers.set("x-user-id", userId);
  const contentType = request.headers.get("content-type");
  if (contentType) headers.set("content-type", contentType);

  const method = request.method.toUpperCase();
  const hasBody = method !== "GET" && method !== "HEAD";
  const body = hasBody ? new Uint8Array(await request.arrayBuffer()) : undefined;

  try {
    const upstream = await fetch(target, {
      method,
      headers,
      body,
      redirect: "manual",
      cache: "no-store",
    });
    const outHeaders = new Headers();
    const upstreamType = upstream.headers.get("content-type");
    if (upstreamType) outHeaders.set("content-type", upstreamType);
    outHeaders.set("cache-control", pathname.startsWith("/api/") ? "no-store" : "public, max-age=3600");
    const bytes = new Uint8Array(await upstream.arrayBuffer());
    return new Response(bytes, { status: upstream.status, headers: outHeaders });
  } catch {
    return Response.json(
      { error: "BACKEND_UNAVAILABLE", message: "后端暂时无法连接" },
      { status: 502 },
    );
  }
}
