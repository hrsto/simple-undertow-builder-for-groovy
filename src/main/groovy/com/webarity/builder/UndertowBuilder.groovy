package com.webarity.builder;

import io.undertow.*
import io.undertow.server.*
import io.undertow.server.handlers.*
import io.undertow.server.protocol.http.HttpOpenListener

import java.nio.*

import org.xnio.*
import org.xnio.channels.AcceptingChannel

import io.undertow.servlet.api.DeploymentInfo
import io.undertow.servlet.api.DeploymentManager
import io.undertow.servlet.api.ServletContainer
import io.undertow.servlet.Servlets as UndertowServlets

/**
* <p>Simple builder for Undertow-based http server with Servlets support.</p>
*
* @author <a mailto:"sto.hristo@gmail.com">Hristo Y. Stoyanov</a>
*/
@groovy.transform.TypeChecked
class UndertowBuilder {

    /**
    * <p></p>
    */
    Xnio xnio

    /**
    * <p>xnio.createWorker</p>
    */
    XnioWorker worker

    /**
    * <p>Undertow options.</p>
    * @see UndertowBuilderOptions
    */
    UndertowBuilderOptions builderOpts

    /**
    * <p>Future updates may allow for multiple Undertow containers.</p>
    */
    List<Container> containers

    /**
    * <p></p>
    */
    HttpOpenListener openListener

    /**
    * <p></p>
    */
    PathHandler serverPathHandler

    /**
    * <p>Server host. Like {@code '127.0.0.1'}</p>
    */
    String host


    /**
    * <p>Server port. Like {@code 8080}</p>
    */
    int port

    /**
    * <p>Server instance itself.</p>
    */
    AcceptingChannel server

    /**
    * <p>Defined in the constructor as mapping between label and true/false whether that step has been completed</p>
    */
    private Map<String, Boolean> startupChecklist

    /**
    * <p>In case user doesn't configure path, do some defaults ones - /kill that stops the server.</p>
    */
    private boolean doDefaultHandler = true

    /**
    * <p>Entry point.</p>
    *
    * @param host hostname or ip address - {@code 127.0.0.1} for ex.
    * @param port which port to listen to for connections
    */
    UndertowBuilder(String host, int port) {
        startupChecklist = new HashMap<>()
        startupChecklist.put('mainBuilder', false)

        this.host = host
        this.port = port

        builderOpts = new UndertowBuilderOptions()
        containers = new LinkedList<>()
        xnio = Xnio.getInstance();
        serverPathHandler = new PathHandler()
    }
    
    /**
    * <p>Main configuration starts here. Used to create multiple depoyments and their associated Servlets.</p>
    * <p>Map args can be:</p>
    * <ul>
    *   <li>containerName - String, becomes the base context path.</li>
    *   <li>deployments - Closure<Deployment>, accepts multiple method calls to {@code deployment([])} to create multiple deployments</li>
    * </ul>
    * <p>Usage example:</p>
    * <pre><code>
    *   new UndertowBuilder('127.0.0.1', 8080)
            .undertow(
                containerName: 'thing',
                deployments: {
                    deployment ([ // can have multiple of this
                        contextPath: 'thingDepl',
                        displayName: 'thingDisplayName'
                    ]) { // this Closure argument is part of the deployment method
                        addServlets([
                            Servlets.servlet("first", TestServlet.class).addMapping("/zzz").addInitParam("fromDepl", "thingDepl"),
                            Servlets.servlet("second", TestServlet.class).addMapping("/zzz/asdf").addInitParam("fromDepl", "thingDepl of the asdf variant")
                        ])
                    }
                }
            )
            .startAll()
    * </pre></code>
    * 
    * @param args closure that delegates to {@link Deployments}
    * @return the builder itself
    */
    UndertowBuilder undertow(Map<String, Closure<Deployments>> args) {
        if (!(args.containerName as String)) throw new Exception("Duplicate container id: $args.containerName")

        Container newContainer = new Container(args.containerName as String)
        containers << newContainer

        Deployments d = new Deployments()
        Closure cl = args.deployments.rehydrate(d, this, this)
        cl.resolveStrategy = Closure.DELEGATE_ONLY
        cl()

        d.deployments.each { newContainer.addDeployment it }

        startupChecklist.mainBuilder = true
        this
    }

    /**
     * <p>Exposes delegates to serverPathHandler and the {@code this} object of the builder class to the closure. By default method calls will be evaluated to the PathHandler, i.e. if in the body of the closure, something like {@code addExactPath(....)} is called, this will be resolved against the serverPathHandler.</p>
     * <p>All the properties of this class are available as well. So from the closure can access the actual {@code containers} list and operate on them at will, as well as objects like {@code openListener}, the xnio worker - {@code worker}, etc. This can be used to implement graceful shutdown of some sort.</p>
     * <p>Example:</p>
     * <pre><code>
     * .withPathHandlers {
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
                    log "fail clean shutdown: ${ex.message}"
                    System.exit(1)
                }
            })
            .addExactPath('/tralala', {
                it.responseSender.send('tralalalala')
            })
        }
     * </code></pre>
     * @param c Closure<PathHandler> whose delegate is the PathHandler itself, while owner/this point to the builder class, exposind its properties like {@codecontainers} - a list of containers.
     */
    UndertowBuilder withPathHandlers(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=PathHandler) Closure<PathHandler> c) {
        doDefaultHandler = false

        def cl = c.rehydrate(serverPathHandler, this, this)
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        this
    }

    /**
     * <p>Exposes several method that can be used to customize Undertow. Purely optional. If not specified, default options will be put in place</p>
     * <p>Example:</p>
     * <pre><code>
     * .setup { // same values as the defaults
            withWorkerOptions([
                (org.xnio.Options.WORKER_IO_THREADS): 2,
                (org.xnio.Options.WORKER_TASK_CORE_THREADS): 2,
                (org.xnio.Options.WORKER_TASK_MAX_THREADS): 2,
                (org.xnio.Options.TCP_NODELAY): true
            ])
            withSocketOptions([
                (org.xnio.Options.WORKER_IO_THREADS): 2,
                (org.xnio.Options.TCP_NODELAY): true,
                (org.xnio.Options.REUSE_ADDRESSES): true
            ])
            withBufferOptions([
                bufferSize: 1024,
                bufferPerRegions: 10
            ])
        }
     * </code></pre>
     * @param cl Closure<UndertowBuilderOptions> whose delegate is the options object itself, while this and owner point to the buidler class, exposing its properties
     * @return the builder itself
     */
    UndertowBuilder setup(Closure<UndertowBuilderOptions> c) {
        def cl = c.rehydrate(builderOpts, this, this)
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        this
    }

    /**
     * <p>Starts the server. Or exits with error if it's called before a mandatory step like:</p>
     * <ul>
     *  <li>.undertow([])
     * </ul>
     * <br/>
     * @return buidler itself
     */
    UndertowBuilder startAll() {

        Pool<ByteBuffer> buffers = new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, builderOpts.bufferOpts.bufferSize, builderOpts.bufferOpts.bufferSize * builderOpts.bufferOpts.bufferPerRegions)

        worker = xnio.createWorker builderOpts.workerOptions

        addDefaultHandler doDefaultHandler

        containers.each {
            it.deployments.each { mgr ->
                mgr.deploy()

                String path = "/$it.containerName/${mgr.getDeployment().getDeploymentInfo().getContextPath()}"

                log "deploying to: $path"

                serverPathHandler.addPath("$path/", mgr.start())
            }
        }

        openListener = new HttpOpenListener(buffers, OptionMap.builder().set(UndertowOptions.BUFFER_PIPELINED_DATA, true).addAll(builderOpts.serverOptions).map)
        openListener.rootHandler = serverPathHandler
        ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ChannelListeners.openListenerAdapter openListener
        AcceptingChannel<? extends StreamConnection> server = worker.createStreamConnectionServer new InetSocketAddress(host, port), acceptListener, builderOpts.socketOptions
        server.resumeAccepts();

        log 'starting...'

        this
    }

    /**
     * <p>Before server is launched, check if all mandatory steps are completed.</p>
     */
    private void doFinalCheck() {
        startupChecklist.each {
            if (! it.value) {
                log 'Required step before startAll() has been skipped.'
                System.exit(1)
            }
        }
    }

    /**
     * <p>Just print to stdout a message.</p>
     */
    private void log(msg) {
        println "-------------------------- $msg"
    }

    /**
     * <p>Attaches a default PathHandler when user have not specified their own PathHandler. This handler simply has a {@code /kill} endpoint that will gracefully terminate and exit the server</p>
     */
    private void addDefaultHandler(boolean doDefaultHandler) {
        if (!doDefaultHandler) return

        log 'Adding default handler due to missing withPathHandlers {} block. It add the exact path /kill which will trigger server shutdown and end process.'

        serverPathHandler.addExactPath('/kill', {
            log 'shutting down'

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
                log "fail clean shutdown: ${ex.message}"
                System.exit 1
            }
        })
    }
}