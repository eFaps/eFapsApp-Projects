/*
 * Copyright 2003 - 2009 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.projects;

import java.util.UUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public enum Projects
{
    /** Projects_Document2DocumentAbstract.*/
    DOC2DOCABSTRACT("3e943f28-3d98-45b3-bdc8-752ed2030eb1"),

    /** Projects_Document2DerivativeDocument.*/
    DOC2DOCDERIVED("c5c7b42c-f1ce-4978-9d0e-d93fa231b585"),

    /** Projects_Project2ObjectAbstract.*/
    PROJECT2OBJECTABSTRACT("bdf673a1-16ae-4c3b-b890-91d2a04c0a0f"),

    /** Projects_Project2DocumentAbstract.*/
    PROJECT2DOCABSTRACT("a6accf51-06d0-4882-a4c7-617cd5bf789b"),

    /** Projects_ProjectService.*/
    SERVICE("bcb8ba16-d485-477b-b198-b95d08f3915a"),

    /** Projects_ProjectService2DocumentAbstract.*/
    SERVICE2DOCABSTRACT("bcb41bad-a349-4012-9cb8-2afd16830aa3"),

    /** Projects_ProjectService2Account.*/
    SERVICE2ACCOUNT("488be94c-657f-4679-99c5-3de8a6759f6f"),

    /** Projects_ProjectService2CreditNote.*/
    SERVICE2CREDITNOTE("d9d8ce4c-03ff-467f-9863-9976033ff1a9"),

    /** Projects_ProjectService2DeliveryNote.*/
    SERVICE2DELIVERYNOTE("80a26199-4c5a-4b93-a1d9-fb7eace93ea7"),

    /** Projects_ProjectService2Invoice.*/
    SERVICE2INVOICE("09ee5526-448a-46e6-bb76-cf6da5aeed82"),

    /** Projects_ProjectService2OrderOutbound.*/
    SERVICE2ORDEROUT("79b7bc09-4d46-48ee-9cba-e794a67df34e"),

    /** Projects_ProjectService2Quotation.*/
    SERVICE2QUOTATION("48e4823f-0dac-4d87-b54e-7acb02c1e460"),

    /** Projects_ProjectService2Receipt.*/
    SERVICE2RECEIPT("6665b0d5-3209-453c-be4a-cc8cb6dce387"),

    /** Projects_ProjectService2RecievingTicket.*/
    SERVICE2RECIEVINGTICKET("8069d557-349c-4ff4-80f1-b8fbd110a4dd"),

    /** Projects_ProjectService2Reminder.*/
    SERVICE2REMINDER("c1362cf7-4609-4536-b27f-1df9be08102b"),

    /** Projects_ProjectService2Request.*/
    SERVICE2REQUEST("80d52d23-708e-469a-9e2e-5ac64527e6e6"),

    /** Projects_ProjectService2GoodsIssueSlip.*/
    SERVICE2GOODSISSUE("9d2b7c31-7d70-4d40-ab2c-7877f497adda"),

    /** Projects_ProjectService2ReturnSlip. */
    SERVICE2RETURNSLIP("de4ede93-630c-429b-83d8-5099dc2775ea"),

    /** Projects_ProjectService2WorkOrder.*/
    SERVICE2WORKORDER("fb13e8f1-a029-4cc1-be80-db3c8972c57e"),

    /** Projects_ProjectService2WorkReport.*/
    SERVICE2WORKREPORT("1961c2e7-6f2c-46a3-9c0e-d8b02b65a858"),

    /** Projects_ServiceRequest.*/
    SERVICEREQUEST("1e3d8c77-c3bf-4ce8-8650-e27cd4c733ea"),

    /** Projects_ServiceRequestStatus.*/
    SERVICEREQUESTSTATUS("a028ae03-b453-4b0b-b051-f7f2d5e81cf9"),

    /** Projects_WorkOrder.*/
    WORKORDER("d2e9428f-0dd9-4cae-b5d0-f3513b1dfbd0"),

    /** Projects_WorkOrderStatus.*/
    WORKORDERSTATUS("964e8a6e-e63d-454f-91e2-057be5627827"),

    /** Projects_WorkReport.*/
    WORKREPORT("ee0dae0f-3b0b-4204-8963-0d2098b9c1bb"),

    /** Projects_WorkReportStatus.*/
    WORKREPORTSTATUS("2f08feb0-c4fa-4d92-8b3c-5cd1562ef54d");

    /**
     * UUID for the Type.
     */
    private final UUID uuid;

    /**
     * @param _uuid string for the uuid
     */
    private Projects(final String _uuid)
    {
        this.uuid = UUID.fromString(_uuid);
    }

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    public UUID getUuid()
    {
        return this.uuid;
    }
}
