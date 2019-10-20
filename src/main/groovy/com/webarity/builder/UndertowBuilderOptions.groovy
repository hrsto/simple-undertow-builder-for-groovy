package com.webarity.builder;

import org.xnio.*

/**
* <p>Encapsulates various Undertow options. Primary usage is with Closures that delegate to instances of this class.</p>
* <p>All the base options are preset to their defaults, so zero config is needed to just launch the server.</p>
*
* @author <a mailto:"sto.hristo@gmail.com">Hristo Y. Stoyanov</a>
*/
@groovy.transform.TypeChecked
class UndertowBuilderOptions {

    private OptionMap.Builder workerOpts
    private OptionMap.Builder socketOpts
    private OptionMap.Builder serverOpts

    Map<String, Integer> bufferOpts

    OptionMap workerOptions
    OptionMap socketOptions
    OptionMap serverOptions


    UndertowBuilderOptions() {
        this.workerOpts = OptionMap.builder()
            .set(Options.WORKER_IO_THREADS, 2)
            .set(Options.WORKER_TASK_CORE_THREADS, 2)
            .set(Options.WORKER_TASK_MAX_THREADS, 2)
            .set(Options.TCP_NODELAY, true)

        this.socketOpts = OptionMap.builder()
            .set(Options.WORKER_IO_THREADS, 2)
            .set(Options.TCP_NODELAY, true)
            .set(Options.REUSE_ADDRESSES, true)

        this.serverOpts = OptionMap.builder()

        bufferOpts = new HashMap<>()
        bufferOpts.bufferSize = 1024
        bufferOpts.bufferPerRegions = 10

        this.workerOptions = this.workerOpts.map
        this.socketOptions = this.socketOpts.map
        this.serverOptions = this.serverOpts.map
    }

    /**
     * <p>{@link org.xnio.XnioWorker XnioWorker} is initialized with these options.</p>
     *
     * @param opts options
     * @return itself
     */
    UndertowBuilderOptions withWorkerOptions(Map<org.xnio.Option<?>, ?> opts) {
        opts.each { this.workerOpts.set it.key, it.value }
        workerOptions = workerOpts.map

        this
    }

    /**
    * <p>{@link org.xnio.AcceptingChannel AcceptingChannel} is initialized with these options via the {@link org.xnio.XnioWorker#createStreamConnectionServer createStreamConnectionServer method}.</p>
    *
    * @param opts options
    * @return itself
    */
    UndertowBuilderOptions withSocketOptions(Map<org.xnio.Option<?>, ?> opts) {
        opts.each { this.socketOpts.set it.key, it.value }
        socketOptions = socketOpts.map

        this
    }

    /**
     * <p>{@link io.undertow.server.protocol.http.HttpOpenListener HttpOpenListener} is initialized with these options.</p>
     *
     * @param opts options
     * @return itself
     */
    UndertowBuilderOptions withServerOptions(Map<org.xnio.Option<?>, ?> opts) {
        opts.each { this.serverOpts.set it.key, it.value }
        serverOptions = serverOpts.map

        this
    }
    /**
     * <p>{@link org.xnio.Pool Pool} is initialized with these options.</p>
     *
     * @param opts options
     * @return itself
     */
    UndertowBuilderOptions withBufferOptions(Map<String, Integer> opts) {
        bufferOpts.bufferSize = opts.bufferSize ?: bufferOpts.bufferSize
        bufferOpts.bufferPerRegions = opts.bufferPerRegions ?: bufferOpts.bufferPerRegions
        bufferOpts

        this
    }

}
