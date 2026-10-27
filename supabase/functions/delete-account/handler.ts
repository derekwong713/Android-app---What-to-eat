export interface AccountApi {
  verify(token: string): Promise<{ id: string; provider?: string } | null>;
  delete(id: string): Promise<boolean>;
}
export function handler(config: { url?: string; key?: string; api: (url: string, key: string) => AccountApi }) {
  return async (request: Request): Promise<Response> => {
    const headers = { "Content-Type": "application/json", "Cache-Control": "no-store" };
    const reply = (status: number, value: unknown) => new Response(JSON.stringify(value), { status, headers });
    if (request.method !== "POST") return reply(405, { error: "method_not_allowed" });
    const token = request.headers.get("Authorization")?.match(/^Bearer (.+)$/)?.[1];
    if (!token) return reply(401, { error: "auth_required" });
    if (!config.url || !config.key) return reply(503, { error: "configuration" });
    try {
      const api = config.api(config.url, config.key);
      const identity = await api.verify(token);
      if (!identity || !["google", "facebook"].includes(identity.provider ?? "")) return reply(401, { error: "auth_required" });
      // Ignore all body IDs. Delete only the identity returned by verified Auth.
      if (!await api.delete(identity.id)) return reply(500, { error: "deletion_failed" });
      return reply(200, { deleted: true });
    } catch { return reply(500, { error: "deletion_failed" }); }
  };
}
