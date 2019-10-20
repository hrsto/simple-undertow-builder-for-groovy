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
import io.undertow.servlet.Servlets

/**
* <p>Encapsulates a list of deployments and method for creation.</p>
*
* @author <a mailto:"sto.hristo@gmail.com">Hristo Y. Stoyanov</a>
*/
@groovy.transform.TypeChecked
class Deployments {

    /**
    * <p>List of deployments.</p>
    */
    List<DeploymentInfo> deployments

    /**
    * <p>Default construct intializes the class with an empty list.</p>
    */
    Deployments() {
        deployments = new LinkedList<>()
    }

    /**
    * <p>Creates a deployment.</p>
    *
    * @param args the following arguments for the map are taken literally and transferred to {@link DeploymentInfo}:
    * <ul>
    *   <li>hostName</li>
    *   <li>contextPath</li>
    *   <li>displayName</li>
    *   <li>serverName</li> 
    *</ul>
    * @param c closure that delegates to {@link DeploymentInfo}
    * @return itself
    */
    Deployments deployment(Map<String, String> args, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=DeploymentInfo)  Closure<DeploymentInfo> c) {
        DeploymentInfo deplInf = Servlets.deployment()

        deplInf.hostName = args.hostName
        deplInf.contextPath = args.contextPath
        deplInf.displayName = args.displayName
        deplInf.serverName = args.serverName
        deplInf.deploymentName = args.deploymentName ?: 'some deployment'

        deplInf.classLoader = this.class.classLoader

        def cl = c.rehydrate(deplInf, this, this)
        cl.resolveStrategy = Closure.DELEGATE_ONLY
        cl()

        deplInf.validate()

        deployments.add deplInf
        
        this
    }
}

