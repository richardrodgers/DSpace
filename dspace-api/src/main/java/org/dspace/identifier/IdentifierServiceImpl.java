/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.sql.SQLException;
import java.util.List;

/**
 * The main service class used to reserve, register and resolve identifiers
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class IdentifierServiceImpl implements IdentifierService {

    private List<IdentifierProvider> providers;

    /** log4j category */
    private static Logger log = Logger.getLogger(IdentifierServiceImpl.class);

    @Autowired
   @Required
   public void setProviders(List<IdentifierProvider> providers)
   {
       this.providers = providers;

       for(IdentifierProvider p : providers)
       {
           p.setParentService(this);
       }
   }

    /**
     * Reserves identifiers for the item
     * @param context dspace context
     * @param dso dspace object
     */
    public void reserve(Context context, DSpaceObject dso) throws AuthorizeException, SQLException, IdentifierException {
        for (IdentifierProvider service : providers)
        {
            service.mint(context, dso);
        }
        //Update our item
        dso.update();
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier) throws AuthorizeException, SQLException, IdentifierException {
        // Next resolve all other services
        for (IdentifierProvider service : providers)
        {
            if(service.supports(identifier))
            {
                service.reserve(context, dso, identifier);
            }
        }
        //Update our item
        dso.update();
    }

    @Override
    public void register(Context context, DSpaceObject dso) throws AuthorizeException, SQLException, IdentifierException {
        //We need to commit our context because one of the providers might require the handle created above
        // Next resolve all other services
        for (IdentifierProvider service : providers)
        {
            service.register(context, dso);
        }
        //Update our item
        dso.update();
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier) throws AuthorizeException, SQLException, IdentifierException {

        //We need to commit our context because one of the providers might require the handle created above
        // Next resolve all other services
        for (IdentifierProvider service : providers)
        {
            service.register(context, object, identifier);
        }
        //Update our item
        object.update();
    }

    @Override
    public String lookup(Context context, DSpaceObject dso, Class<? extends Identifier> identifier) {
        for (IdentifierProvider service : providers)
        {
            if(service.supports(identifier))
            {
               try{
                   String result = service.lookup(context, dso);
                   if (result != null){
                       return result;
                   }
               } catch (IdentifierException e) {
                   log.error(e.getMessage(),e);
               }
            }
        }
        return null;
    }

    public DSpaceObject resolve(Context context, String identifier) throws IdentifierNotFoundException, IdentifierNotResolvableException{
        for (IdentifierProvider service : providers)
        {
            if(service.supports(identifier))
            {   try
                {
                    DSpaceObject result = service.resolve(context, identifier);
                    if (result != null)
                    {
                        return result;
                    }
                } catch (IdentifierException e) {
                    log.error(e.getMessage(),e);
                }
            }

        }
        return null;
    }

    public void delete(Context context, DSpaceObject dso) throws AuthorizeException, SQLException, IdentifierException {
       for (IdentifierProvider service : providers)
       {
            try
            {
                service.delete(context, dso);
            } catch (IdentifierException e) {
                log.error(e.getMessage(),e);
            }
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier) throws AuthorizeException, SQLException, IdentifierException {
        for (IdentifierProvider service : providers)
        {
            try
            {
                if(service.supports(identifier))
                {
                    service.delete(context, dso, identifier);
                }
            } catch (IdentifierException e) {
                log.error(e.getMessage(),e);
            }
        }
    }
}
