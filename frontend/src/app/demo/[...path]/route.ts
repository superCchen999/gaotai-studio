import { proxyRequest } from "@/lib/proxy";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

type Context = { params: Promise<{ path: string[] }> };

export async function GET(request: Request, context: Context) {
  const { path } = await context.params;
  return proxyRequest(request, `/demo/${path.map(encodeURIComponent).join("/")}`);
}
