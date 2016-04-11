package gov.watertown_ny.barcodes.executer;

import gov.watertown_ny.barcodes.model.bcode;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
//import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.extensions.surf.util.I18NUtil;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.detector.MultiDetector;
import com.google.zxing.qrcode.decoder.Decoder;

/**
 * @author Brian Phelps
 *
 */
public class BarCodeActionExecuter extends ActionExecuterAbstractBase {
	
	protected NodeService nodeService;
	protected ContentService contentService;
	
	protected com.google.zxing.BarcodeFormat bCodeFormat;

	public BarCodeActionExecuter() {
		
	}

	/**
	 * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
	 */
	@Override
	protected void executeImpl(Action action, NodeRef node) {
		long t = System.currentTimeMillis();
		Integer pageNumber = (Integer)action.getParameterValue("pageNumber");
		if (pageNumber == null){
			pageNumber = 1;
		}
		List<com.google.zxing.BarcodeFormat> bCodeTypeList = new ArrayList<com.google.zxing.BarcodeFormat>();
		String bCodeTypeParam = (String) action.getParameterValue("bCodeType");
		if (bCodeTypeParam != null){
			if (!bCodeTypeParam.isEmpty()) {
				bCodeTypeList.add(BarcodeFormat.valueOf(bCodeTypeParam));
			}
		}
		
		
		//final Logger logger = Logger.getLogger(getClass());
		QName barCodeAspect = QName.createQNameWithValidLocalName(bcode.NAMESPACE_BARCODE_CONTENT_MODEL, bcode.ASPECT_BCODE_BARCODED);
		QName barCodeTextProp = QName.createQNameWithValidLocalName(bcode.NAMESPACE_BARCODE_CONTENT_MODEL, bcode.PROP_BARCODETEXT);

		//logger.info(I18NUtil.getMessage("get-barcode.title"));
	
		ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
	    InputStream nodeStream = reader.getContentInputStream();
	    ContentData contentData = (ContentData) nodeService.getProperty(node, ContentModel.PROP_CONTENT);
	    String mimeType = contentData.getMimetype();
		nodeService.addAspect(node, barCodeAspect, null);
		

		Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
		

		hints.put(DecodeHintType.TRY_HARDER,true);
		if (bCodeTypeList.size()>0)	hints.put(DecodeHintType.POSSIBLE_FORMATS,bCodeTypeList);
		
		int imageType = BufferedImage.TYPE_INT_RGB;
        int resolution = 300;
        BufferedImage image;
        switch (mimeType){
        case "application/pdf":
        	image = pdfToBufferedImage(nodeStream, resolution, imageType, pageNumber);
        	break;
        case "image/png":
        	image = pngToBufferedImage(nodeStream);
        	break;
        default:
        	image=null;
        }
		if (image!=null)
		{
			BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
			HybridBinarizer binarizer = new HybridBinarizer(source);
			//GlobalHistogramBinarizer binarizer = new GlobalHistogramBinarizer(source);
			BinaryBitmap bitmap = new BinaryBitmap(binarizer);
			try {BitMatrix matrix = bitmap.getBlackMatrix();
				MultiDetector detector = new MultiDetector(matrix);
				DetectorResult[] results = detector.detectMulti(hints);
				System.out.println(node.toString());
				System.out.println("Results in " + (System.currentTimeMillis()-t) + " msec");
				Decoder qrDecoder = new Decoder();
			
			
				for (DetectorResult result : results)
				{	
				
					System.out.println("Result:");
	            
					System.out.println(result.getBits().toString());
					// DecoderResult qrResult = qrDecoder.decode(result.getBits());
					System.out.println(qrDecoder.decode(result.getBits()).getText());
					for (ResultPoint point : result.getPoints())
						{
						System.out.println("  Point " + point.getX() + "," + point.getY());
						}
				}

			} catch (ChecksumException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  catch (NotFoundException e) {
				nodeService.setProperty(node, barCodeTextProp, "nothing yet with hybrid");
				}
			try{	System.out.println("does it get here?");
				Result qrCodeResult = new MultiFormatReader().decode(bitmap,hints);
				nodeService.setProperty(node, barCodeTextProp, qrCodeResult.getText());
			} catch (NotFoundException e) {
			nodeService.setProperty(node, barCodeTextProp, "nothing yet with hybrid");
			}
		}
		else
			{
				nodeService.setProperty(node, barCodeTextProp, I18NUtil.getMessage("get-barcode.wrongMimeType")+" "+mimeType);
			}
		
		
		
		
		

		
	

	}
	private BufferedImage pngToBufferedImage(InputStream nodeStream){
		try {
				return ImageIO.read(nodeStream);
			} catch (IOException e) {
				return null;
			}
		
		}
	
	private BufferedImage pdfToBufferedImage(InputStream nodeStream, int resolution, int imageType, int pageNumber){
		PDDocument document;
		try {
			document = PDDocument.load(nodeStream);
			List<?> pages = document.getDocumentCatalog().getAllPages();
	        PDPage page = (PDPage) pages.get(pageNumber-1);
	        BufferedImage image = page.convertToImage(imageType, resolution);
	        document.close();
	        return image;
			
		} catch (IOException e) {
		
			e.printStackTrace();
			return null;
		}

		}

	/**
	 * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
	 */
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {

		
		paramList.add(																
				new ParameterDefinitionImpl("pageNumber", DataTypeDefinition.INT, false,getParamDisplayLabel("pageNumber")));
		paramList.add(
				new ParameterDefinitionImpl("bCodeType", DataTypeDefinition.ANY, false,getParamDisplayLabel("bCodeType"),false, "barCodeType-content-properties"));

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
