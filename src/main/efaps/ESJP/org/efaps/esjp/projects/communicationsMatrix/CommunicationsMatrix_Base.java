package org.efaps.esjp.projects.communicationsMatrix;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormProjects;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.util.EFapsException;


public class CommunicationsMatrix_Base
{

    public Return create4CommunicationsMatrix(final Parameter _parameter)
        throws EFapsException
    {
        final Instance projectInst = _parameter.getInstance();
        final Insert insert = new Insert(CIProjects.CommunicationsMatrix);
        insert.add(CIProjects.CommunicationsMatrix.ProjectAbstractLink, projectInst.getId());
       insert.add(CIProjects.CommunicationsMatrix.Contact,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.contact.name));
        insert.add(CIProjects.CommunicationsMatrix.Email,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.email.name));
        insert.add(CIProjects.CommunicationsMatrix.Phone,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.phone.name));
        insert.add(CIProjects.CommunicationsMatrix.Origin,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.origin.name));
        final String pointOfContactLink = _parameter
                        .getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.pointOfContact.name);
        if (pointOfContactLink != null) {
            final Instance pointOfContactInst = Instance.get(pointOfContactLink);
            if (pointOfContactInst.isValid()) {
                insert.add(CIProjects.CommunicationsMatrix.PointOfContact, pointOfContactInst.getId());
            }
        }
        insert.execute();
        return new Return();
    }
}
