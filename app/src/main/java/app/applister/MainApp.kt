package app.applister

import android.app.Application

object App {
    lateinit var ctx: Application
}

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(applicationContext)
        App.ctx = this
    }
}
