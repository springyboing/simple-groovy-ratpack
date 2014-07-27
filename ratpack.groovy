@GrabResolver("http://jcenter.bintray.com")
@GrabResolver("http://oss.jfrog.org/artifactory/repo")
@Grab("io.ratpack:ratpack-groovy:0.9.6")
@Grab("io.ratpack:ratpack-rx:0.9.6")
@Grab("io.ratpack:ratpack-hystrix:0.9.6")
@Grab("io.ratpack:ratpack-jackson:0.9.6")

import groovy.json.JsonSlurper
import ratpack.hystrix.HystrixRatpack
import ratpack.rx.RxRatpack
import ratpack.jackson.JacksonModule
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixObservableCommand
import ratpack.http.client.HttpClients
import ratpack.http.client.ReceivedResponse
import ratpack.launch.LaunchConfig
import ratpack.error.ServerErrorHandler
import rx.*

import static ratpack.jackson.Jackson.json
import static ratpack.groovy.Groovy.*
import static ratpack.rx.RxRatpack.observe

ratpack {

    bindings {
        add new JacksonModule()

        init {
            RxRatpack.initialize()
            HystrixRatpack.initialize()
        }
        //bind ServerErrorHandler
    }


    handlers {
        get {
            response.send "This is the app root (also try: /api/ping and /api/test?deps=http://localhost:5050/api/ping)"
        }

        get("api/ping") {
            println "Incoming ping request: "
            render json([body: 'pong', port: launchConfig.port, publicAddress: launchConfig.publicAddress])
        }

        get("api/test") {

            Observable.from(request.queryParams.getAll('deps'))
                    .map { url ->
                println 'url: ' + url
                def start = System.currentTimeMillis()
                def body = new JsonSlurper().parseText(url.toURL().text)
                def end = System.currentTimeMillis()

                [url: url, response: body, time: end - start]
            }
            .toList()
                    .subscribe({ // onNext
                println "Sub: " + it
                render json(it)
            }, { // onError
                println("onError: " + it.printStackTrace())
            }, { // onCompleted
                println("onCompleted")
            })

        }

        assets "public"
    }
}