import { createClient } from "https://esm.sh/@supabase/supabase-js@2.57.4";
import { handler } from "./handler.ts";
// Privileged keys remain in the server environment. JWT is verified by Auth, not trusted from request body.
Deno.serve(handler({
  url: Deno.env.get("SUPABASE_URL"), key: Deno.env.get("SUPABASE_SERVICE_ROLE_KEY"),
  api(url, key) {
    const admin = createClient(url, key, { auth: { persistSession: false, autoRefreshToken: false } });
    return {
      async verify(token) {
        const { data, error } = await admin.auth.getUser(token);
        return error || !data.user ? null : { id: data.user.id, provider: data.user.app_metadata.provider };
      },
      async delete(id) { const { error } = await admin.auth.admin.deleteUser(id, false); return !error; },
    };
  },
}));
