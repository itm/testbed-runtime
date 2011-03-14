
package eu.wisebed.testbed.api.wsn.v22;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.wisebed.testbed.api.wsn.v22 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetPropertyValueOfResponse_QNAME = new QName("urn:WSNService", "getPropertyValueOfResponse");
    private final static QName _GetFiltersResponse_QNAME = new QName("urn:WSNService", "getFiltersResponse");
    private final static QName _DescribeCapabilities_QNAME = new QName("urn:WSNService", "describeCapabilities");
    private final static QName _FlashProgramsResponse_QNAME = new QName("urn:WSNService", "flashProgramsResponse");
    private final static QName _DisableNode_QNAME = new QName("urn:WSNService", "disableNode");
    private final static QName _GetNeighbourhoodResponse_QNAME = new QName("urn:WSNService", "getNeighbourhoodResponse");
    private final static QName _DisableNodeResponse_QNAME = new QName("urn:WSNService", "disableNodeResponse");
    private final static QName _DefineNetwork_QNAME = new QName("urn:WSNService", "defineNetwork");
    private final static QName _DestroyVirtualLinkResponse_QNAME = new QName("urn:WSNService", "destroyVirtualLinkResponse");
    private final static QName _EnablePhysicalLink_QNAME = new QName("urn:WSNService", "enablePhysicalLink");
    private final static QName _DisablePhysicalLinkResponse_QNAME = new QName("urn:WSNService", "disablePhysicalLinkResponse");
    private final static QName _SendResponse_QNAME = new QName("urn:WSNService", "sendResponse");
    private final static QName _AddController_QNAME = new QName("urn:WSNService", "addController");
    private final static QName _GetNetworkResponse_QNAME = new QName("urn:CommonTypes", "getNetworkResponse");
    private final static QName _EnableNodeResponse_QNAME = new QName("urn:WSNService", "enableNodeResponse");
    private final static QName _UnknownNodeUrnException_QNAME = new QName("urn:WSNService", "UnknownNodeUrnException");
    private final static QName _UnsupportedOperationException_QNAME = new QName("urn:WSNService", "UnsupportedOperationException");
    private final static QName _FlashPrograms_QNAME = new QName("urn:WSNService", "flashPrograms");
    private final static QName _AreNodesAlive_QNAME = new QName("urn:WSNService", "areNodesAlive");
    private final static QName _Program_QNAME = new QName("urn:WSNService", "program");
    private final static QName _ResetNodesResponse_QNAME = new QName("urn:WSNService", "resetNodesResponse");
    private final static QName _RemoveController_QNAME = new QName("urn:WSNService", "removeController");
    private final static QName _SetVirtualLink_QNAME = new QName("urn:WSNService", "setVirtualLink");
    private final static QName _GetNeighbourhood_QNAME = new QName("urn:WSNService", "getNeighbourhood");
    private final static QName _RemoveControllerResponse_QNAME = new QName("urn:WSNService", "removeControllerResponse");
    private final static QName _GetFilters_QNAME = new QName("urn:WSNService", "getFilters");
    private final static QName _EnableNode_QNAME = new QName("urn:WSNService", "enableNode");
    private final static QName _AddControllerResponse_QNAME = new QName("urn:WSNService", "addControllerResponse");
    private final static QName _DisablePhysicalLink_QNAME = new QName("urn:WSNService", "disablePhysicalLink");
    private final static QName _SetStartTimeResponse_QNAME = new QName("urn:WSNService", "setStartTimeResponse");
    private final static QName _Send_QNAME = new QName("urn:WSNService", "send");
    private final static QName _EnablePhysicalLinkResponse_QNAME = new QName("urn:WSNService", "enablePhysicalLinkResponse");
    private final static QName _SetVirtualLinkResponse_QNAME = new QName("urn:WSNService", "setVirtualLinkResponse");
    private final static QName _DestroyVirtualLink_QNAME = new QName("urn:WSNService", "destroyVirtualLink");
    private final static QName _GetVersionResponse_QNAME = new QName("urn:WSNService", "getVersionResponse");
    private final static QName _Message_QNAME = new QName("urn:CommonTypes", "message");
    private final static QName _GetVersion_QNAME = new QName("urn:WSNService", "getVersion");
    private final static QName _Urn_QNAME = new QName("urn:CommonTypes", "urn");
    private final static QName _DescribeCapabilitiesResponse_QNAME = new QName("urn:WSNService", "describeCapabilitiesResponse");
    private final static QName _DefineNetworkResponse_QNAME = new QName("urn:WSNService", "defineNetworkResponse");
    private final static QName _ResetNodes_QNAME = new QName("urn:WSNService", "resetNodes");
    private final static QName _GetNetwork_QNAME = new QName("urn:CommonTypes", "getNetwork");
    private final static QName _AreNodesAliveResponse_QNAME = new QName("urn:WSNService", "areNodesAliveResponse");
    private final static QName _SetStartTime_QNAME = new QName("urn:WSNService", "setStartTime");
    private final static QName _ProgramMetaData_QNAME = new QName("urn:WSNService", "programMetaData");
    private final static QName _GetPropertyValueOf_QNAME = new QName("urn:WSNService", "getPropertyValueOf");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.wisebed.testbed.api.wsn.v22
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetNetworkResponse }
     * 
     */
    public GetNetworkResponse createGetNetworkResponse() {
        return new GetNetworkResponse();
    }

    /**
     * Create an instance of {@link SetVirtualLinkResponse }
     * 
     */
    public SetVirtualLinkResponse createSetVirtualLinkResponse() {
        return new SetVirtualLinkResponse();
    }

    /**
     * Create an instance of {@link AreNodesAlive }
     * 
     */
    public AreNodesAlive createAreNodesAlive() {
        return new AreNodesAlive();
    }

    /**
     * Create an instance of {@link SetStartTime }
     * 
     */
    public SetStartTime createSetStartTime() {
        return new SetStartTime();
    }

    /**
     * Create an instance of {@link AreNodesAliveResponse }
     * 
     */
    public AreNodesAliveResponse createAreNodesAliveResponse() {
        return new AreNodesAliveResponse();
    }

    /**
     * Create an instance of {@link ProgramMetaData }
     * 
     */
    public ProgramMetaData createProgramMetaData() {
        return new ProgramMetaData();
    }

    /**
     * Create an instance of {@link FlashProgramsResponse }
     * 
     */
    public FlashProgramsResponse createFlashProgramsResponse() {
        return new FlashProgramsResponse();
    }

    /**
     * Create an instance of {@link GetVersion }
     * 
     */
    public GetVersion createGetVersion() {
        return new GetVersion();
    }

    /**
     * Create an instance of {@link GetNeighbourhood }
     * 
     */
    public GetNeighbourhood createGetNeighbourhood() {
        return new GetNeighbourhood();
    }

    /**
     * Create an instance of {@link RemoveControllerResponse }
     * 
     */
    public RemoveControllerResponse createRemoveControllerResponse() {
        return new RemoveControllerResponse();
    }

    /**
     * Create an instance of {@link UnknownNodeUrnException }
     * 
     */
    public UnknownNodeUrnException createUnknownNodeUrnException() {
        return new UnknownNodeUrnException();
    }

    /**
     * Create an instance of {@link SetVirtualLink }
     * 
     */
    public SetVirtualLink createSetVirtualLink() {
        return new SetVirtualLink();
    }

    /**
     * Create an instance of {@link EnablePhysicalLinkResponse }
     * 
     */
    public EnablePhysicalLinkResponse createEnablePhysicalLinkResponse() {
        return new EnablePhysicalLinkResponse();
    }

    /**
     * Create an instance of {@link Send }
     * 
     */
    public Send createSend() {
        return new Send();
    }

    /**
     * Create an instance of {@link DisablePhysicalLinkResponse }
     * 
     */
    public DisablePhysicalLinkResponse createDisablePhysicalLinkResponse() {
        return new DisablePhysicalLinkResponse();
    }

    /**
     * Create an instance of {@link FlashPrograms }
     * 
     */
    public FlashPrograms createFlashPrograms() {
        return new FlashPrograms();
    }

    /**
     * Create an instance of {@link GetFilters }
     * 
     */
    public GetFilters createGetFilters() {
        return new GetFilters();
    }

    /**
     * Create an instance of {@link Program }
     * 
     */
    public Program createProgram() {
        return new Program();
    }

    /**
     * Create an instance of {@link DefineNetwork }
     * 
     */
    public DefineNetwork createDefineNetwork() {
        return new DefineNetwork();
    }

    /**
     * Create an instance of {@link GetFiltersResponse }
     * 
     */
    public GetFiltersResponse createGetFiltersResponse() {
        return new GetFiltersResponse();
    }

    /**
     * Create an instance of {@link DescribeCapabilities }
     * 
     */
    public DescribeCapabilities createDescribeCapabilities() {
        return new DescribeCapabilities();
    }

    /**
     * Create an instance of {@link DestroyVirtualLink }
     * 
     */
    public DestroyVirtualLink createDestroyVirtualLink() {
        return new DestroyVirtualLink();
    }

    /**
     * Create an instance of {@link SetStartTimeResponse }
     * 
     */
    public SetStartTimeResponse createSetStartTimeResponse() {
        return new SetStartTimeResponse();
    }

    /**
     * Create an instance of {@link AddControllerResponse }
     * 
     */
    public AddControllerResponse createAddControllerResponse() {
        return new AddControllerResponse();
    }

    /**
     * Create an instance of {@link RemoveController }
     * 
     */
    public RemoveController createRemoveController() {
        return new RemoveController();
    }

    /**
     * Create an instance of {@link EnableNodeResponse }
     * 
     */
    public EnableNodeResponse createEnableNodeResponse() {
        return new EnableNodeResponse();
    }

    /**
     * Create an instance of {@link EnablePhysicalLink }
     * 
     */
    public EnablePhysicalLink createEnablePhysicalLink() {
        return new EnablePhysicalLink();
    }

    /**
     * Create an instance of {@link DisableNodeResponse }
     * 
     */
    public DisableNodeResponse createDisableNodeResponse() {
        return new DisableNodeResponse();
    }

    /**
     * Create an instance of {@link SendResponse }
     * 
     */
    public SendResponse createSendResponse() {
        return new SendResponse();
    }

    /**
     * Create an instance of {@link DestroyVirtualLinkResponse }
     * 
     */
    public DestroyVirtualLinkResponse createDestroyVirtualLinkResponse() {
        return new DestroyVirtualLinkResponse();
    }

    /**
     * Create an instance of {@link EnableNode }
     * 
     */
    public EnableNode createEnableNode() {
        return new EnableNode();
    }

    /**
     * Create an instance of {@link DefineNetworkResponse }
     * 
     */
    public DefineNetworkResponse createDefineNetworkResponse() {
        return new DefineNetworkResponse();
    }

    /**
     * Create an instance of {@link ResetNodes }
     * 
     */
    public ResetNodes createResetNodes() {
        return new ResetNodes();
    }

    /**
     * Create an instance of {@link GetNetwork }
     * 
     */
    public GetNetwork createGetNetwork() {
        return new GetNetwork();
    }

    /**
     * Create an instance of {@link GetPropertyValueOfResponse }
     * 
     */
    public GetPropertyValueOfResponse createGetPropertyValueOfResponse() {
        return new GetPropertyValueOfResponse();
    }

    /**
     * Create an instance of {@link AddController }
     * 
     */
    public AddController createAddController() {
        return new AddController();
    }

    /**
     * Create an instance of {@link DescribeCapabilitiesResponse }
     * 
     */
    public DescribeCapabilitiesResponse createDescribeCapabilitiesResponse() {
        return new DescribeCapabilitiesResponse();
    }

    /**
     * Create an instance of {@link DisableNode }
     * 
     */
    public DisableNode createDisableNode() {
        return new DisableNode();
    }

    /**
     * Create an instance of {@link Message }
     * 
     */
    public Message createMessage() {
        return new Message();
    }

    /**
     * Create an instance of {@link UnsupportedOperationException }
     * 
     */
    public UnsupportedOperationException createUnsupportedOperationException() {
        return new UnsupportedOperationException();
    }

    /**
     * Create an instance of {@link ResetNodesResponse }
     * 
     */
    public ResetNodesResponse createResetNodesResponse() {
        return new ResetNodesResponse();
    }

    /**
     * Create an instance of {@link GetPropertyValueOf }
     * 
     */
    public GetPropertyValueOf createGetPropertyValueOf() {
        return new GetPropertyValueOf();
    }

    /**
     * Create an instance of {@link DisablePhysicalLink }
     * 
     */
    public DisablePhysicalLink createDisablePhysicalLink() {
        return new DisablePhysicalLink();
    }

    /**
     * Create an instance of {@link GetVersionResponse }
     * 
     */
    public GetVersionResponse createGetVersionResponse() {
        return new GetVersionResponse();
    }

    /**
     * Create an instance of {@link GetNeighbourhoodResponse }
     * 
     */
    public GetNeighbourhoodResponse createGetNeighbourhoodResponse() {
        return new GetNeighbourhoodResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPropertyValueOfResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getPropertyValueOfResponse")
    public JAXBElement<GetPropertyValueOfResponse> createGetPropertyValueOfResponse(GetPropertyValueOfResponse value) {
        return new JAXBElement<GetPropertyValueOfResponse>(_GetPropertyValueOfResponse_QNAME, GetPropertyValueOfResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFiltersResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getFiltersResponse")
    public JAXBElement<GetFiltersResponse> createGetFiltersResponse(GetFiltersResponse value) {
        return new JAXBElement<GetFiltersResponse>(_GetFiltersResponse_QNAME, GetFiltersResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DescribeCapabilities }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "describeCapabilities")
    public JAXBElement<DescribeCapabilities> createDescribeCapabilities(DescribeCapabilities value) {
        return new JAXBElement<DescribeCapabilities>(_DescribeCapabilities_QNAME, DescribeCapabilities.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FlashProgramsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "flashProgramsResponse")
    public JAXBElement<FlashProgramsResponse> createFlashProgramsResponse(FlashProgramsResponse value) {
        return new JAXBElement<FlashProgramsResponse>(_FlashProgramsResponse_QNAME, FlashProgramsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisableNode }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "disableNode")
    public JAXBElement<DisableNode> createDisableNode(DisableNode value) {
        return new JAXBElement<DisableNode>(_DisableNode_QNAME, DisableNode.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNeighbourhoodResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getNeighbourhoodResponse")
    public JAXBElement<GetNeighbourhoodResponse> createGetNeighbourhoodResponse(GetNeighbourhoodResponse value) {
        return new JAXBElement<GetNeighbourhoodResponse>(_GetNeighbourhoodResponse_QNAME, GetNeighbourhoodResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisableNodeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "disableNodeResponse")
    public JAXBElement<DisableNodeResponse> createDisableNodeResponse(DisableNodeResponse value) {
        return new JAXBElement<DisableNodeResponse>(_DisableNodeResponse_QNAME, DisableNodeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DefineNetwork }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "defineNetwork")
    public JAXBElement<DefineNetwork> createDefineNetwork(DefineNetwork value) {
        return new JAXBElement<DefineNetwork>(_DefineNetwork_QNAME, DefineNetwork.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetVirtualLinkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "destroyVirtualLinkResponse")
    public JAXBElement<SetVirtualLinkResponse> createDestroyVirtualLinkResponse(SetVirtualLinkResponse value) {
        return new JAXBElement<SetVirtualLinkResponse>(_DestroyVirtualLinkResponse_QNAME, SetVirtualLinkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnablePhysicalLink }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "enablePhysicalLink")
    public JAXBElement<EnablePhysicalLink> createEnablePhysicalLink(EnablePhysicalLink value) {
        return new JAXBElement<EnablePhysicalLink>(_EnablePhysicalLink_QNAME, EnablePhysicalLink.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisablePhysicalLinkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "disablePhysicalLinkResponse")
    public JAXBElement<DisablePhysicalLinkResponse> createDisablePhysicalLinkResponse(DisablePhysicalLinkResponse value) {
        return new JAXBElement<DisablePhysicalLinkResponse>(_DisablePhysicalLinkResponse_QNAME, DisablePhysicalLinkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SendResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "sendResponse")
    public JAXBElement<SendResponse> createSendResponse(SendResponse value) {
        return new JAXBElement<SendResponse>(_SendResponse_QNAME, SendResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddController }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "addController")
    public JAXBElement<AddController> createAddController(AddController value) {
        return new JAXBElement<AddController>(_AddController_QNAME, AddController.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNetworkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:CommonTypes", name = "getNetworkResponse")
    public JAXBElement<GetNetworkResponse> createGetNetworkResponse(GetNetworkResponse value) {
        return new JAXBElement<GetNetworkResponse>(_GetNetworkResponse_QNAME, GetNetworkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnableNodeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "enableNodeResponse")
    public JAXBElement<EnableNodeResponse> createEnableNodeResponse(EnableNodeResponse value) {
        return new JAXBElement<EnableNodeResponse>(_EnableNodeResponse_QNAME, EnableNodeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UnknownNodeUrnException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "UnknownNodeUrnException")
    public JAXBElement<UnknownNodeUrnException> createUnknownNodeUrnException(UnknownNodeUrnException value) {
        return new JAXBElement<UnknownNodeUrnException>(_UnknownNodeUrnException_QNAME, UnknownNodeUrnException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UnsupportedOperationException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "UnsupportedOperationException")
    public JAXBElement<UnsupportedOperationException> createUnsupportedOperationException(UnsupportedOperationException value) {
        return new JAXBElement<UnsupportedOperationException>(_UnsupportedOperationException_QNAME, UnsupportedOperationException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FlashPrograms }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "flashPrograms")
    public JAXBElement<FlashPrograms> createFlashPrograms(FlashPrograms value) {
        return new JAXBElement<FlashPrograms>(_FlashPrograms_QNAME, FlashPrograms.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AreNodesAlive }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "areNodesAlive")
    public JAXBElement<AreNodesAlive> createAreNodesAlive(AreNodesAlive value) {
        return new JAXBElement<AreNodesAlive>(_AreNodesAlive_QNAME, AreNodesAlive.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Program }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "program")
    public JAXBElement<Program> createProgram(Program value) {
        return new JAXBElement<Program>(_Program_QNAME, Program.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResetNodesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "resetNodesResponse")
    public JAXBElement<ResetNodesResponse> createResetNodesResponse(ResetNodesResponse value) {
        return new JAXBElement<ResetNodesResponse>(_ResetNodesResponse_QNAME, ResetNodesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveController }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "removeController")
    public JAXBElement<RemoveController> createRemoveController(RemoveController value) {
        return new JAXBElement<RemoveController>(_RemoveController_QNAME, RemoveController.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetVirtualLink }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "setVirtualLink")
    public JAXBElement<SetVirtualLink> createSetVirtualLink(SetVirtualLink value) {
        return new JAXBElement<SetVirtualLink>(_SetVirtualLink_QNAME, SetVirtualLink.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNeighbourhood }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getNeighbourhood")
    public JAXBElement<GetNeighbourhood> createGetNeighbourhood(GetNeighbourhood value) {
        return new JAXBElement<GetNeighbourhood>(_GetNeighbourhood_QNAME, GetNeighbourhood.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveControllerResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "removeControllerResponse")
    public JAXBElement<RemoveControllerResponse> createRemoveControllerResponse(RemoveControllerResponse value) {
        return new JAXBElement<RemoveControllerResponse>(_RemoveControllerResponse_QNAME, RemoveControllerResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFilters }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getFilters")
    public JAXBElement<GetFilters> createGetFilters(GetFilters value) {
        return new JAXBElement<GetFilters>(_GetFilters_QNAME, GetFilters.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnableNode }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "enableNode")
    public JAXBElement<EnableNode> createEnableNode(EnableNode value) {
        return new JAXBElement<EnableNode>(_EnableNode_QNAME, EnableNode.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddControllerResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "addControllerResponse")
    public JAXBElement<AddControllerResponse> createAddControllerResponse(AddControllerResponse value) {
        return new JAXBElement<AddControllerResponse>(_AddControllerResponse_QNAME, AddControllerResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisablePhysicalLink }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "disablePhysicalLink")
    public JAXBElement<DisablePhysicalLink> createDisablePhysicalLink(DisablePhysicalLink value) {
        return new JAXBElement<DisablePhysicalLink>(_DisablePhysicalLink_QNAME, DisablePhysicalLink.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetStartTimeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "setStartTimeResponse")
    public JAXBElement<SetStartTimeResponse> createSetStartTimeResponse(SetStartTimeResponse value) {
        return new JAXBElement<SetStartTimeResponse>(_SetStartTimeResponse_QNAME, SetStartTimeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Send }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "send")
    public JAXBElement<Send> createSend(Send value) {
        return new JAXBElement<Send>(_Send_QNAME, Send.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnablePhysicalLinkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "enablePhysicalLinkResponse")
    public JAXBElement<EnablePhysicalLinkResponse> createEnablePhysicalLinkResponse(EnablePhysicalLinkResponse value) {
        return new JAXBElement<EnablePhysicalLinkResponse>(_EnablePhysicalLinkResponse_QNAME, EnablePhysicalLinkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetVirtualLinkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "setVirtualLinkResponse")
    public JAXBElement<SetVirtualLinkResponse> createSetVirtualLinkResponse(SetVirtualLinkResponse value) {
        return new JAXBElement<SetVirtualLinkResponse>(_SetVirtualLinkResponse_QNAME, SetVirtualLinkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DestroyVirtualLink }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "destroyVirtualLink")
    public JAXBElement<DestroyVirtualLink> createDestroyVirtualLink(DestroyVirtualLink value) {
        return new JAXBElement<DestroyVirtualLink>(_DestroyVirtualLink_QNAME, DestroyVirtualLink.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetVersionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getVersionResponse")
    public JAXBElement<GetVersionResponse> createGetVersionResponse(GetVersionResponse value) {
        return new JAXBElement<GetVersionResponse>(_GetVersionResponse_QNAME, GetVersionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Message }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:CommonTypes", name = "message")
    public JAXBElement<Message> createMessage(Message value) {
        return new JAXBElement<Message>(_Message_QNAME, Message.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetVersion }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getVersion")
    public JAXBElement<GetVersion> createGetVersion(GetVersion value) {
        return new JAXBElement<GetVersion>(_GetVersion_QNAME, GetVersion.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:CommonTypes", name = "urn")
    public JAXBElement<String> createUrn(String value) {
        return new JAXBElement<String>(_Urn_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DescribeCapabilitiesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "describeCapabilitiesResponse")
    public JAXBElement<DescribeCapabilitiesResponse> createDescribeCapabilitiesResponse(DescribeCapabilitiesResponse value) {
        return new JAXBElement<DescribeCapabilitiesResponse>(_DescribeCapabilitiesResponse_QNAME, DescribeCapabilitiesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DefineNetworkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "defineNetworkResponse")
    public JAXBElement<DefineNetworkResponse> createDefineNetworkResponse(DefineNetworkResponse value) {
        return new JAXBElement<DefineNetworkResponse>(_DefineNetworkResponse_QNAME, DefineNetworkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResetNodes }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "resetNodes")
    public JAXBElement<ResetNodes> createResetNodes(ResetNodes value) {
        return new JAXBElement<ResetNodes>(_ResetNodes_QNAME, ResetNodes.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNetwork }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:CommonTypes", name = "getNetwork")
    public JAXBElement<GetNetwork> createGetNetwork(GetNetwork value) {
        return new JAXBElement<GetNetwork>(_GetNetwork_QNAME, GetNetwork.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AreNodesAliveResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "areNodesAliveResponse")
    public JAXBElement<AreNodesAliveResponse> createAreNodesAliveResponse(AreNodesAliveResponse value) {
        return new JAXBElement<AreNodesAliveResponse>(_AreNodesAliveResponse_QNAME, AreNodesAliveResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetStartTime }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "setStartTime")
    public JAXBElement<SetStartTime> createSetStartTime(SetStartTime value) {
        return new JAXBElement<SetStartTime>(_SetStartTime_QNAME, SetStartTime.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ProgramMetaData }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "programMetaData")
    public JAXBElement<ProgramMetaData> createProgramMetaData(ProgramMetaData value) {
        return new JAXBElement<ProgramMetaData>(_ProgramMetaData_QNAME, ProgramMetaData.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPropertyValueOf }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getPropertyValueOf")
    public JAXBElement<GetPropertyValueOf> createGetPropertyValueOf(GetPropertyValueOf value) {
        return new JAXBElement<GetPropertyValueOf>(_GetPropertyValueOf_QNAME, GetPropertyValueOf.class, null, value);
    }

}
