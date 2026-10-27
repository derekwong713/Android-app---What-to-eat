package hk.whatoeat
import hk.whatoeat.domain.draw.StartupAdGate
import hk.whatoeat.data.auth.OAuthProtocol
import hk.whatoeat.core.model.*
import org.junit.Assert.*
import org.junit.Test
class StartupAndOAuthTest {
 @Test fun lateAdNeverShowsAfterHomeOrNavigation() {val gate=StartupAdGate(1800);gate.timeout();assertFalse(gate.loaded(2000));gate.finish();assertFalse(gate.loaded(10))}
 @Test fun duplicateCallbacksCannotRepeatAd() {val gate=StartupAdGate(1800);assertTrue(gate.loaded(100));assertFalse(gate.loaded(101));gate.timeout();assertFalse(gate.home);gate.finish();assertFalse(gate.loaded(102))}
 @Test fun exactDeadlineFailsOpen() {val gate=StartupAdGate(1800);assertFalse(gate.loaded(1800));assertTrue(gate.home)}
 @Test fun pkceMatchesRfcVector() {assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",OAuthProtocol.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))}
 @Test fun callbackRequiresCorrectNonceAndScheme() {assertEquals("code",OAuthProtocol.callback("hk.whatoeat://auth/callback?flow=nonce&code=code","hk.whatoeat","nonce"));for(uri in listOf("other://auth/callback?flow=nonce&code=code","hk.whatoeat://auth/callback?flow=wrong&code=code")) {try {OAuthProtocol.callback(uri,"hk.whatoeat","nonce");fail()} catch(_: AppFailure) {}}}
 @Test fun oauthCancellationIsDistinct() {try {OAuthProtocol.callback("hk.whatoeat://auth/callback?flow=n&error=access_denied","hk.whatoeat","n");fail()} catch(e: AppFailure) {assertEquals(Failure.Cancelled,e.kind)}}
}
