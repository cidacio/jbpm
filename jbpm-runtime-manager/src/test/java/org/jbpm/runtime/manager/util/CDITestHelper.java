package org.jbpm.runtime.manager.util;

import java.util.Properties;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.jbpm.shared.services.impl.JbpmJTATransactionManager;
import org.kie.api.io.ResourceType;
import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceNio2WrapperImpl;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.cdi.qualifier.PerProcessInstance;
import org.kie.internal.runtime.manager.cdi.qualifier.PerRequest;
import org.kie.internal.runtime.manager.cdi.qualifier.Singleton;
import org.kie.internal.task.api.TaskService;

@ApplicationScoped
public class CDITestHelper {

    private EntityManagerFactory emf;
    
    
    @Produces
    @Singleton
    @PerRequest
    @PerProcessInstance
    public RuntimeEnvironment produceEnvironment(EntityManagerFactory emf) {
        Properties properties= new Properties();
        properties.setProperty("mary", "HR");
        properties.setProperty("john", "HR");
        RuntimeEnvironment environment = RuntimeEnvironmentBuilder.getDefault()
                .entityManagerFactory(emf)
                .userGroupCallback(new JBossUserGroupCallbackImpl(properties))
                .addAsset(ResourceFactory.newClassPathResource("BPMN2-ScriptTask.bpmn2"), ResourceType.BPMN2)
                .addAsset(ResourceFactory.newClassPathResource("BPMN2-UserTask.bpmn2"), ResourceType.BPMN2)
                .get();
        return environment;
    }
    
    @Produces
    public EntityManagerFactory produceEntityManagerFactory() {
        if (this.emf == null) {
            this.emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa"); 
        }
        
        return this.emf;
    }
    
    @Produces
    @ApplicationScoped
    public EntityManager getEntityManager() {
        EntityManager em = produceEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();
        return em;
    }

    @ApplicationScoped
    public void commitAndClose(@Disposes EntityManager em) {
        try {
            em.getTransaction().commit();
            em.close();
        } catch (Exception e) {

        }
    }
    
    @Produces
    public Logger createLogger(InjectionPoint injectionPoint) {
        return Logger.getLogger(injectionPoint.getMember()
                .getDeclaringClass().getName());
    }
    
    @Produces
    @Named("ioStrategy")
    public IOService createIOService(){
        return new IOServiceNio2WrapperImpl();
    }
    
    @Produces
    public TaskService newTaskService(@Singleton RuntimeEnvironment runtimeEnvironment) {

        TaskService internalTaskService =   HumanTaskServiceFactory.newTaskServiceConfigurator()
        .transactionManager(new JbpmJTATransactionManager())
        .entityManagerFactory(emf)
        .userGroupCallback(runtimeEnvironment.getUserGroupCallback())
        .getTaskService();
                    
        return internalTaskService;

    }
}
