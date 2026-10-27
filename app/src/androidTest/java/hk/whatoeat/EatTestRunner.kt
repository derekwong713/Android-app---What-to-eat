package hk.whatoeat
import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
class EatTestRunner: AndroidJUnitRunner() {
 override fun newApplication(cl: ClassLoader,className: String,context: Context): Application {System.setProperty("whatoeat.test.noAds","true");return super.newApplication(cl,className,context)}
}
