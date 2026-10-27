import { handler, type AccountApi } from "./handler.ts";
function assert(value: unknown) { if (!value) throw new Error("Assertion failed"); }
const request = (body = {}) => new Request("https://local.invalid/delete", { method: "POST", headers: { Authorization: "Bearer test-token" }, body: JSON.stringify(body) });
function setup(provider: string | undefined, valid = true, deleted = true) {
  const ids: string[] = [];
  const api: AccountApi = { verify: async () => valid ? { id: "verified-user", provider } : null, delete: async id => { ids.push(id); return deleted; } };
  return { ids, run: handler({ url: "https://unit.invalid", key: "server-only-test-placeholder", api: () => api }) };
}
Deno.test("reject method and missing bearer without touching admin API", async () => {
  const run = handler({ url: "url", key: "key", api: () => { throw new Error("Should not construct API"); } });
  assert((await run(new Request("https://unit.invalid"))).status === 405);
  assert((await run(new Request("https://unit.invalid", { method: "POST" }))).status === 401);
});
Deno.test("missing server configuration is explicit", async () => { assert((await handler({ api: () => { throw 0; } })(request())).status === 503); });
Deno.test("only verified Google and Facebook identities can delete themselves", async () => {
  for (const provider of ["google", "facebook"]) { const t = setup(provider); assert((await t.run(request({ user_id: "victim" }))).status === 200); assert(t.ids.join() === "verified-user"); }
});
Deno.test("invalid bearer and email provider cannot delete", async () => {
  for (const t of [setup("google", false), setup("email"), setup(undefined)]) { assert((await t.run(request())).status === 401); assert(t.ids.length === 0); }
});
Deno.test("admin deletion errors do not leak secret or user details", async () => {
  const t = setup("google", true, false); const response = await t.run(request()); assert(response.status === 500); assert(await response.text() === '{"error":"deletion_failed"}');
});
