package hk.whatoeat.domain.draw
/** One cold-start opportunity. Once home is entered no delayed callback may show an ad. */
class StartupAdGate(private val deadline: Long) {
    var home=false;private set
    var showing=false;private set
    fun loaded(now: Long): Boolean {
        if(home || showing || now>=deadline) {if(!showing) home=true;return false}
        showing=true;return true
    }
    fun timeout() {if(!showing) home=true}
    fun finish() {showing=false;home=true}
}
