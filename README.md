# Simple Undertow builder for Groovy

Easily build configurable [Undertow](https://www.undertow.io) servers with multiple deployments in Groovy.

## Usage

Get the dependency and then:

```groovy
new UndertowBuilder('127.0.0.1', 8080)
    .undertow(
        containerName: 'thing', // becomes the root path
        deployments: {
            deployment ([
                contextPath: 'firstDeployment',
                displayName: 'display-name'
            ]) {
                addServlets([
                    Servlets.servlet("first", TestServlet.class).addMapping("/zzz").addInitParam("some-param", "firstDeployment"),
                    Servlets.servlet("second", TestServlet.class).addMapping("/zzz/second").addInitParam("some-param", "firstDeployment of the second servlet")
                ])
            }
            deployment ([
                hostName: 'localhost',
                contextPath: 'secondDeployment',
                serverName: 'thingServlet',
                displayName: 'display-name'
            ]) {
                addServlet(Servlets.servlet(TestServlet.class).addMapping("/aaa").addInitParam("some-param", "secondDeployment"))
            }
        }
    )
    .setup {
        withWorkerOptions([
            (org.xnio.Options.WORKER_IO_THREADS): 5,
            (org.xnio.Options.WORKER_TASK_CORE_THREADS): 2,
            (org.xnio.Options.WORKER_TASK_MAX_THREADS): 2,
            (org.xnio.Options.TCP_NODELAY): true
        ])
        withSocketOptions([
            (org.xnio.Options.WORKER_IO_THREADS): 5,
            (org.xnio.Options.TCP_NODELAY): true,
            (org.xnio.Options.REUSE_ADDRESSES): true
        ])
        withBufferOptions([
            bufferSize: 1024,
            bufferPerRegions: 10
        ])
    }
    .withPathHandlers {
        addExactPath('/kill', {
            println 'shutting down'

            try {
                it.responseSender.send('shutting down')

                containers.each {
                    it.deployments.each {
                        it.stop()
                        it.undeploy()
                    }
                }
                worker.shutdownNow()
            } catch (ex) {
                println "fail clean shutdown: ${ex.message}"
                System.exit(1)
            }
        })
        .addExactPath('/ping', {
            it.responseSender.send('pong')
        })
    }
    .startAll()
```

## Details

The `setup` and/or `withPathHandlers` blocks are purely optional. If `withPathHandlers` is omitted, a default path `/kill` will be included that will perform a clean shutdown. It's essentially the same as in the example above.

More details in the api docs.

---

<https://www.webarity.com>