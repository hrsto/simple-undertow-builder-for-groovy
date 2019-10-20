package com.webarity.builder

import io.undertow.servlet.api.DeploymentInfo
import io.undertow.servlet.api.ServletContainer
import io.undertow.servlet.api.DeploymentManager
import io.undertow.servlet.Servlets

/**
* <p>Simple builder for Undertow-based http server with Servlets support.</p>
* 
* @author <a mailto:"sto.hristo@gmail.com">Hristo Y. Stoyanov</a>
*/
@groovy.transform.TypeChecked
class Container {

    /**
    * <p>Name of the container. Taken as the value for the root context so that: {@link name-of-container/servlet-context/...}</p>
    */
    String containerName

    /**
    * <p></p>
    */
    ServletContainer container

    /**
    * <p>List of initialized Servlet deployments.</p>
    */
    List<DeploymentManager> deployments

    /**
    * <p>Initializes this class with a {@link #containerName container name}</p>
    *
    * @param containerName {@link #containerName}
    */
    Container(String containerName) {
        this.containerName = containerName
        this.container = Servlets.newContainer()
        deployments = new LinkedList<>()
    }

    /**
    *
    * @param depl Servlet deployment to add
    */
    void addDeployment(DeploymentInfo depl) {
        deployments << (container.addDeployment(depl))
        println "added a deployment, now having ${deployments.size()} deployments"
    }
}