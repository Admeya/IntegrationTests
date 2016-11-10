package com.fico.vtb.testcase.phase6.microflow.RA;

import com.fico.vtb.checks.*;
import com.fico.vtb.test.bo.*;
import com.fico.vtb.test.core.*;
import com.fico.vtb.test.core.util.CollectionHelper;
import com.fico.vtb.test.core.util.ConfigProperties;
import com.fico.vtb.test.core.util.ReadXML;
import com.fico.vtb.test.dao.*;
import com.fico.vtb.test.service.IOMEntryAsyncRequestExport;
import com.fico.vtb.test.service.IOMEntryExport1;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.xml.soap.SOAPMessage;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class HappyPath {
    final static Logger logger = Logger.getLogger(HappyPath.class);

    public BaseService iomEntryAsyncRequestExportRA;
    public BaseService iomEntryExport1CPP;
    public BaseService iomEntryExport1ICH;
    public BaseService iomEntryExport1BCS;
    public BaseService iomEntryExport1BCS2;
    public BaseService iomEntryExport1AG;
    public BaseService iomEntryExport1CRE;
    public BaseService iomEntryExport1RBG;

    public CreditApplicationDAO creditApplicationDAO;
    public Om4XmlStorageDAO om4XmlStorageDAO;
    public Om4AsyncRequestDAO om4AsyncRequestDAO;
    public MockDataCaptureDAO mockDataCaptureDAO;

    public ApplicationBO applicationBO;

    private final static String UNIQUE_ID = "UNIQUE_ID";

    @BeforeClass
    public void Init() {
        iomEntryAsyncRequestExportRA = new IOMEntryAsyncRequestExport(ActionNames.RA, true);

        iomEntryExport1CPP = new IOMEntryExport1(ActionNames.CPPR1);
        iomEntryExport1ICH = new IOMEntryExport1(ActionNames.ICHR1);
        iomEntryExport1BCS = new IOMEntryExport1(ActionNames.BCSR1);
        iomEntryExport1BCS2 = new IOMEntryExport1(ActionNames.BCS2R1);
        iomEntryExport1AG = new IOMEntryExport1(ActionNames.AGRR1);
        iomEntryExport1CRE = new IOMEntryExport1(ActionNames.CRER1);
        iomEntryExport1RBG = new IOMEntryExport1(ActionNames.RBGR1);


        creditApplicationDAO = new CreditApplicationDAO();
        om4XmlStorageDAO = new Om4XmlStorageDAO();
        om4AsyncRequestDAO = new Om4AsyncRequestDAO();
        mockDataCaptureDAO = new MockDataCaptureDAO();

        if (applicationBO == null) {
            applicationBO = new ApplicationBO("/applications/phase6/RA_MORT.xml", "RA_MORT" + "_" + Calendar.getInstance().getTimeInMillis());

            Map<String, DMResponse> dm = new HashMap<String, DMResponse>();
            dm.put("DM1", DMResponse.NO_DECISION);
            dm.put("DM2", DMResponse.INVESTIGATE);
            dm.put("DM3", DMResponse.NO_DECISION);
            dm.put("DM4", DMResponse.APPROVE);

            try {
                new MockConfigDAO().setDMResponses(applicationBO.getApplicationId(), dm);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Test(description = "invoke SOAP request with application data.")
    public void serviceIsInvoked() {
        if (iomEntryAsyncRequestExportRA == null) {
            Assert.fail("Service is not initialized");
        }

        if (applicationBO == null) {
            Assert.fail("BO is not initialized");
        }

        try {
            iomEntryAsyncRequestExportRA.invokeOneWay(applicationBO);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = {"serviceIsInvoked"}, description = "Checks if application was stored in the DB.")
    public void applicationIsStored() {
        boolean isExist = false;
        for (int i = 0; i < 6; i++) {
            try {
                isExist = creditApplicationDAO.isApplicationExists(applicationBO.getApplicationId());
                if (isExist) {
                    break;
                }
            } catch (SQLException e) {
                Assert.fail(e.getMessage());
                e.printStackTrace();
                return;
            }

            try {
                Thread.sleep(Integer.valueOf(ConfigProperties.getProperty("INVOKE.TIMEOUT")));
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        Assert.assertTrue(isExist);
    }

    @Test(dependsOnMethods = {"applicationIsStored"}, description = "Checks number of XMLs that were stored in the DB.")
    public void xmlsAreCreated() {
        XmlStorageCheck.byCount(om4XmlStorageDAO, applicationBO, 2);
    }

    @Test(dependsOnMethods = {"xmlsAreCreated"})
    public void xmlForFesReqIsCreated() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.FES_REQUEST);
    }

    @Test(dependsOnMethods = {"xmlForFesReqIsCreated"})
    public void xmlForSendStatusToFESIsCreated() {
        MockDataCaptureChecks.waitForCountCapturedData(mockDataCaptureDAO, applicationBO, "IOMEntryAsyncResponse", 1);
    }

    @Test(dependsOnMethods = {"xmlForSendStatusToFESIsCreated"})
    public void requestsForExternalServicesCreated() {
        if (RequestsCheck.requestCount(om4AsyncRequestDAO, applicationBO, null, applicationBO.getApplicantsCount() * 4)) {
            RequestsCheck.fillRequests(om4AsyncRequestDAO, applicationBO);
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    @Test(dependsOnMethods = {"requestsForExternalServicesCreated"})
    public void sendCppRequests() {
        int i = 1;
        for (String uuid : CollectionHelper.getKeysByValue(applicationBO.getRequestIds(), ActionNames.CPPR1)) {

            CppBO cppBO = new CppBO("/responses/phase6/CPP.xml", applicationBO.getApplicationId(), "K" + i + "_" + applicationBO.getApplicationId(), uuid);
            try {
                SOAPMessage soapMessage = iomEntryExport1CPP.invoke(cppBO);

                String message = ReadXML.getSoapXmlResponse(soapMessage);

                if (message.equals(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY)) {
                    logger.info(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY);
                } else {
                    Assert.fail(message);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
                return;
            }
            i++;
        }
    }


    @Test(dependsOnMethods = {"sendCppRequests"})
    public void checkCppRequestStatusesInDB() {
        RequestsCheck.status1(om4AsyncRequestDAO, applicationBO, ActionNames.CPPR1);
    }


    @Test(dependsOnMethods = {"checkCppRequestStatusesInDB"})
    public void sendIchRequests() {
        int i = 1;
        for (String uuid : CollectionHelper.getKeysByValue(applicationBO.getRequestIds(), ActionNames.ICHR1)) {
            IchBO ichBO = new IchBO("/responses/phase6/ICH1.xml", applicationBO.getApplicationId(), "K" + i + "_" + applicationBO.getApplicationId(), uuid);
            try {
                SOAPMessage soapMessage = iomEntryExport1ICH.invoke(ichBO);

                String message = ReadXML.getSoapXmlResponse(soapMessage);

                if (message.equals(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY)) {
                    logger.info(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY);
                } else {
                    Assert.fail(message);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
                return;
            }
            i++;
        }
    }

    @Test(dependsOnMethods = {"sendIchRequests"})
    public void checkIchRequestStatusesInDB() {
        RequestsCheck.status1(om4AsyncRequestDAO, applicationBO, ActionNames.ICHR1);
    }


    @Test(dependsOnMethods = {"checkIchRequestStatusesInDB"})
    public void sendAgrRequests() {
        int i = 1;
        for (String uuid : CollectionHelper.getKeysByValue(applicationBO.getRequestIds(), ActionNames.AGRR1)) {

            AgrBO agrBO = new AgrBO("/responses/phase6/AGR.xml", applicationBO.getApplicationId(), "K" + i + "_" + applicationBO.getApplicationId(), uuid);
            try {
                SOAPMessage soapMessage = iomEntryExport1AG.invoke(agrBO);

                String message = ReadXML.getSoapXmlResponse(soapMessage);

                if (message.equals(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY)) {
                    logger.info(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY);
                } else {
                    Assert.fail(message);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
                return;
            }
            i++;
        }

    }

    @Test(dependsOnMethods = {"sendAgrRequests"})
    public void checkAgrRequestStatusesInDB() {
        RequestsCheck.status1(om4AsyncRequestDAO, applicationBO, ActionNames.AGRR1);
    }


    @Test(dependsOnMethods = {"checkAgrRequestStatusesInDB"})
    public void sendBcsResponse() {
        String uuid = CollectionHelper.getKeyByValue(applicationBO.getRequestIds(), ActionNames.BCSR1);

        BcsBO bcsBO = new BcsBO("/responses/phase6/BCS.xml", applicationBO.getApplicationId(), "K1" + "_" + applicationBO.getApplicationId(), uuid);

        try {
            SOAPMessage soapMessage = iomEntryExport1BCS.invoke(bcsBO);

            String message = ReadXML.getSoapXmlResponse(soapMessage);

            if (message.equals(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY)) {
                logger.info(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY);
            } else {
                Assert.fail(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test(dependsOnMethods = {"sendBcsResponse"})
    public void checkRequestStatusesInDB() {
        RequestsCheck.status1(om4AsyncRequestDAO, applicationBO, ActionNames.BCSR1);
    }

    @Test(dependsOnMethods = {"checkRequestStatusesInDB"})
    public void requestsForCreCreated() {
        if (RequestsCheck.requestCount(om4AsyncRequestDAO, applicationBO, ActionNames.CRER1, applicationBO.getApplicantsCount())) {
            RequestsCheck.fillRequests(om4AsyncRequestDAO, applicationBO);
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    @Test(dependsOnMethods = {"requestsForCreCreated"})
    public void statusIntegrationIsPRC2() {
        ApplicationStatusCheck.checkStatus(creditApplicationDAO, applicationBO, DomainValues.VTB_STATUS_INTEGRATION_PRC2, DomainValues.VTB_STAGE_01, DomainValues.VTB_APPLICATION_STATUS_07);
    }

    @Test(dependsOnMethods = {"statusIntegrationIsPRC2"})
    public void DM0RequestStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM0_REQUEST);
    }

    @Test(dependsOnMethods = {"DM0RequestStoredInTheXMLStorage"})
    public void DM0ResponseStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM0_RESPONSE);
    }

    @Test(dependsOnMethods = {"DM0ResponseStoredInTheXMLStorage"})
    public void DM1RequestStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM1_REQUEST);
    }

    @Test(dependsOnMethods = {"DM1RequestStoredInTheXMLStorage"})
    public void DM1ResponseStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM1_RESPONSE);
    }


    @Test(dependsOnMethods = {"DM1ResponseStoredInTheXMLStorage"})
    public void sendCreRequests() {
        String uuid = CollectionHelper.getKeyByValue(applicationBO.getRequestIds(), ActionNames.CRER1);

        CreBO creBO = new CreBO("/responses/phase6/CRE.xml", applicationBO.getApplicationId(), "K1" + "_" + applicationBO.getApplicationId(), uuid);

        try {
            SOAPMessage soapMessage = iomEntryExport1CRE.invoke(creBO);

            String message = ReadXML.getSoapXmlResponse(soapMessage);

            if (message.equals(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY)) {
                logger.info(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY);
            } else {
                Assert.fail(message);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            return;
        }
    }

    @Test(dependsOnMethods = {"sendCreRequests"})
    public void checkCreRequestStatusesInDB() {
        RequestsCheck.status1(om4AsyncRequestDAO, applicationBO, ActionNames.CRER1);
    }


    @Test(dependsOnMethods = {"checkCreRequestStatusesInDB"})
    public void checkBcs2RequestCreatedInDB() {
        if (RequestsCheck.requestCount(om4AsyncRequestDAO, applicationBO, ActionNames.BCS2R1, 1)) {
            RequestsCheck.fillRequests(om4AsyncRequestDAO, applicationBO);
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    @Test(dependsOnMethods = {"checkBcs2RequestCreatedInDB"})
    public void statusIntegrationIsPRC3() {
        ApplicationStatusCheck.checkStatus(creditApplicationDAO, applicationBO, DomainValues.VTB_STATUS_INTEGRATION_PRC3, DomainValues.VTB_STAGE_02, DomainValues.VTB_APPLICATION_STATUS_07);
    }

    @Test(dependsOnMethods = {"statusIntegrationIsPRC3"})
    public void DM2RequestStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM2_REQUEST);
    }

    @Test(dependsOnMethods = {"DM2RequestStoredInTheXMLStorage"})
    public void DM2ResponseStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM2_RESPONSE);
    }

    @Test(dependsOnMethods = {"DM2ResponseStoredInTheXMLStorage"})
    public void WaitFor1stSendStatusToFESIsCreated() {
        MockDataCaptureChecks.waitForCountCapturedData(mockDataCaptureDAO, applicationBO, "invokePH3", 1);
    }

    @Test(dependsOnMethods = {"WaitFor1stSendStatusToFESIsCreated"})
    public void CheckDM2InvestigateSendStatusToFES() {
        MockDataCaptureChecks.checkAppStageAndStatus(mockDataCaptureDAO, applicationBO, "invokePH3", UNIQUE_ID, "02", "07");
    }

    @Test(dependsOnMethods = {"CheckDM2InvestigateSendStatusToFES"})
    public void sendBcs2Request() {
        String uuid = CollectionHelper.getKeyByValue(applicationBO.getRequestIds(), ActionNames.BCS2R1);

        BcsBO bcsBO = new BcsBO("/responses/phase6/BCS2.xml", applicationBO.getApplicationId(), "K1" + "_" + applicationBO.getApplicationId(), uuid);

        try {
            SOAPMessage soapMessage = iomEntryExport1BCS2.invoke(bcsBO);

            String message = ReadXML.getSoapXmlResponse(soapMessage);

            if (message.equals(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY)) {
                logger.info(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY);
            } else {
                Assert.fail(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    @Test(dependsOnMethods = {"sendBcs2Request"})
    public void checkBCS2RequestStatusesInDB() {
        RequestsCheck.status1(om4AsyncRequestDAO, applicationBO, ActionNames.BCS2R1);
    }


    @Test(dependsOnMethods = {"checkBCS2RequestStatusesInDB"})
    public void checkRbgRequestCreatedInDB() {
        if (RequestsCheck.requestCount(om4AsyncRequestDAO, applicationBO, ActionNames.RBGR1, 1)) {
            RequestsCheck.fillRequests(om4AsyncRequestDAO, applicationBO);
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    @Test(dependsOnMethods = {"checkRbgRequestCreatedInDB"})
    public void statusIntegrationIsPRC4() {
        ApplicationStatusCheck.checkStatus(creditApplicationDAO, applicationBO, DomainValues.VTB_STATUS_INTEGRATION_PRC4, DomainValues.VTB_STAGE_03, DomainValues.VTB_APPLICATION_STATUS_07);
    }

    @Test(dependsOnMethods = {"statusIntegrationIsPRC4"})
    public void DM3RequestStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM3_REQUEST);
    }

    @Test(dependsOnMethods = {"DM3RequestStoredInTheXMLStorage"})
    public void DM3ResponseStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM3_RESPONSE);
    }

    @Test(dependsOnMethods = {"DM3ResponseStoredInTheXMLStorage"})
    public void WaitFor2ndSendStatusToFESIsCreated() {
        MockDataCaptureChecks.waitForCountCapturedData(mockDataCaptureDAO, applicationBO, "invokePH3", 2);
    }

    @Test(dependsOnMethods = {"WaitFor2ndSendStatusToFESIsCreated"})
    public void CheckDM3ContinueSendStatusToFES() {
        MockDataCaptureChecks.checkAppStageAndStatus(mockDataCaptureDAO, applicationBO, "invokePH3", UNIQUE_ID, "03", "07");

    }


    @Test(dependsOnMethods = {"CheckDM3ContinueSendStatusToFES"})
    public void sendRbgRequest() {
        String uuid = CollectionHelper.getKeyByValue(applicationBO.getRequestIds(), ActionNames.RBGR1);

        RbgBO rbgBO = new RbgBO("/responses/phase6/RBG.xml", applicationBO.getApplicationId(), "K1" + "_" + applicationBO.getApplicationId(), uuid);

        try {
            SOAPMessage soapMessage = iomEntryExport1RBG.invoke(rbgBO);

            String message = ReadXML.getSoapXmlResponse(soapMessage);

            if (message.equals(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY)) {
                logger.info(SoapResponseConstants.MICROFLOW_INVOKED_SUCCESSFULLY);
            } else {
                Assert.fail(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    @Test(dependsOnMethods = {"sendRbgRequest"})
    public void checkRBGRequestStatusesInDB() {
        RequestsCheck.status1(om4AsyncRequestDAO, applicationBO, ActionNames.RBGR1);
    }

    @Test(dependsOnMethods = {"checkRBGRequestStatusesInDB"})
    public void statusIntegrationIsPRC5() {
        ApplicationStatusCheck.checkStatus(creditApplicationDAO, applicationBO, DomainValues.VTB_STATUS_INTEGRATION_PRC5, DomainValues.VTB_STAGE_05, DomainValues.VTB_APPLICATION_STATUS_01);
    }


    @Test(dependsOnMethods = {"statusIntegrationIsPRC5"})
    public void DM4RequestStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM4_REQUEST);
    }

    @Test(dependsOnMethods = {"DM4RequestStoredInTheXMLStorage"})
    public void DM4ResponseStoredInTheXMLStorage() {
        XmlStorageCheck.atLeastOne(om4XmlStorageDAO, applicationBO, DomainValues.DM4_RESPONSE);
    }

    @Test(dependsOnMethods = {"DM4ResponseStoredInTheXMLStorage"})
    public void WaitFor3rdSendStatusToFESIsCreated() {
        MockDataCaptureChecks.waitForCountCapturedData(mockDataCaptureDAO, applicationBO, "invokePH3", 3);
    }

    @Test(dependsOnMethods = {"WaitFor3rdSendStatusToFESIsCreated"})
    public void CheckDM4ApproveSendStatusToFES() {
        MockDataCaptureChecks.checkAppStageAndStatus(mockDataCaptureDAO, applicationBO, "invokePH3", UNIQUE_ID, "05", "01");
    }
}
