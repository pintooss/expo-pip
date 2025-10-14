package expo.modules.pip

import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

const val moduleName = "ExpoPip"

class ExpoPipModule : Module(), PictureInPictureHelperListener {
  override fun definition() = ModuleDefinition {
   
    Name(moduleName)

    Events("onPipModeChange")

    Function<Boolean>("isInPipMode", this@ExpoPipModule::isInPipMode)

    Function("setPictureInPictureParams", this@ExpoPipModule::setPictureInPictureParams)

    Function("enterPipMode", this@ExpoPipModule::enterPipMode)

    OnCreate(this@ExpoPipModule::attachFragment)

    OnDestroy(this@ExpoPipModule::detachFragment)
      
  }

    private fun buildPictureInPictureParams(options: ParamsRecord): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pictureInPictureParamsBuilder = PictureInPictureParams.Builder()

            options.width?.let { width ->
                options.height?.let { height ->
                    val ratio = Rational(width, height)
                    pictureInPictureParamsBuilder.setAspectRatio(ratio)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                options.title?.let(pictureInPictureParamsBuilder::setTitle)
                options.subtitle?.let(pictureInPictureParamsBuilder::setSubtitle)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                options.seamlessResizeEnabled?.let(pictureInPictureParamsBuilder::setSeamlessResizeEnabled)
                options.autoEnterEnabled?.let(pictureInPictureParamsBuilder::setAutoEnterEnabled)
            }

            options.sourceRectHint?.let {
                val rect = Rect(it.left,it.top, it.right,it.bottom)
                pictureInPictureParamsBuilder.setSourceRectHint(rect)
            }

            return pictureInPictureParamsBuilder.build()
        }
        return null
    }

    private fun setPictureInPictureParams(options: ParamsRecord) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pictureInPictureParamsBuilder = buildPictureInPictureParams(options)

            pictureInPictureParamsBuilder?.let {
                appContext.currentActivity?.setPictureInPictureParams(it)
            }
        }
    }

    private fun enterPipMode(options: ParamsRecord) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pictureInPictureParams = buildPictureInPictureParams(options)

            pictureInPictureParams?.let {
                appContext.currentActivity?.enterPictureInPictureMode(it)
            }
        }
    }

    private fun isInPipMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.currentActivity?.isInPictureInPictureMode == true
        } else {
            false
        }
    }

    fun attachFragment(){
        (appContext.currentActivity as? FragmentActivity)?.let { activity ->
            val fragment = PictureInPictureHelperFragment()
            fragment.setListener(this)
            activity.supportFragmentManager
                .beginTransaction()
                .add(fragment, PictureInPictureHelperFragment.id)
                .commit()
        }
    }

    fun detachFragment(){
        (appContext.currentActivity as? FragmentActivity)?.let { activity ->
            val fragment = activity.supportFragmentManager
                .findFragmentByTag(PictureInPictureHelperFragment.id)

            fragment?.let { fragment ->
                activity.supportFragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }

    override fun onPictureInPictureModeChange(isInPictureInPictureMode: Boolean) {
        this@ExpoPipModule.sendEvent(
            "onPipModeChange",
            bundleOf("isInPipMode" to isInPictureInPictureMode)
        )
    }
    
}