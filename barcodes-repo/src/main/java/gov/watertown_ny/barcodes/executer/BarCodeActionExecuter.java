package gov.watertown_ny.barcodes.executer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
//import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;

import gov.watertown_ny.barcodes.model.bcode;

public class BarCodeActionExecuter extends ActionExecuterAbstractBase {
	
	protected NodeService nodeService;
	protected ContentService contentService;

	public BarCodeActionExecuter() {
		
	}

	@Override
	protected void executeImpl(Action action, NodeRef node) {
		//final Logger logger = Logger.getLogger(getClass());
		QName barCodeAspect = QName.createQNameWithValidLocalName(bcode.NAMESPACE_BARCODE_CONTENT_MODEL, bcode.ASPECT_BCODE_BARCODED);
		QName barCodeTextProp = QName.createQNameWithValidLocalName(bcode.NAMESPACE_BARCODE_CONTENT_MODEL, bcode.PROP_BARCODETEXT);
		//Map<QName, Serializable> properties = nodeService.getProperties(node);
		
		//logger.info(barCodeAspect.getPrefixString());
		//logger.info(barCodeAspect.getNamespaceURI());
		//logger.info(properties.toString());
	
		ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
	    InputStream nodeStream = reader.getContentInputStream();
	    ContentData contentData = (ContentData) nodeService.getProperty(node, ContentModel.PROP_CONTENT);
	    String mimeType = contentData.getMimetype();
		nodeService.addAspect(node, barCodeAspect, null);
		//logger.info(mimeType);

		Map<DecodeHintType, Boolean> decHintMap = new HashMap<DecodeHintType, Boolean>();
		decHintMap.put(DecodeHintType.TRY_HARDER,true);
		int imageType = BufferedImage.TYPE_INT_RGB;
        int resolution = 300;
        BufferedImage image;
        switch (mimeType){
        case "application/pdf":
        	image = pdfToBufferedImage(nodeStream, resolution, imageType);
        	break;
        case "image/png":
        	image = pngToBufferedImage(nodeStream);
        	break;
        default:
        	image=null;
        }
		if (image!=null)
		{
			BufferedImageLuminanceSource bils = new BufferedImageLuminanceSource(image);
			BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(bils));
			try {
				Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap,decHintMap);
				nodeService.setProperty(node, barCodeTextProp, qrCodeResult.getText());
			} catch (NotFoundException e) {
				
				nodeService.setProperty(node, barCodeTextProp, "No Barcode Found");
			}
		}
		else
			{
				nodeService.setProperty(node, barCodeTextProp, "Barcode reading is not configured for document type : "+mimeType);
			}
		
		
		
		
		

		
	

	}
	private BufferedImage pngToBufferedImage(InputStream nodeStream){
		try {
				return ImageIO.read(nodeStream);
			} catch (IOException e) {
				return null;
			}
		
		}
	
	private BufferedImage pdfToBufferedImage(InputStream nodeStream, int resolution, int imageType){
		PDDocument document;
		try {
			document = PDDocument.load(nodeStream);
			List<?> pages = document.getDocumentCatalog().getAllPages();
	        PDPage page = (PDPage) pages.get(0);
	        BufferedImage image = page.convertToImage(imageType, resolution);
	        document.close();
	        return image;
			
		} catch (IOException e) {
		
			e.printStackTrace();
			return null;
		}

		}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
	

	}
	/**
	* @param nodeService The NodeService to set.
	*/
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	/**
	* @param contentService The ContentService to set.
	*/
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

}
