# Alfresco-Zxing-Reader
Uses the Zxing (Zebra Crossing) java libraries to decode bar codes and QR codes in documents.
Currently works for PDF and PNG files only.

A custom aspect (bcode:barCoded) contains a single property (bcode:barCodeText) of type d:text is added.
Retrieval of the bar/QR code text is through a custom action (get-barcode).

Share customization includes the action being added to the document list actions (PDF and PNG only) and the 
addition of an indicator showing those files with the barCoded aspect.

Limitations:
PDF and PNG only (should be easy to add other types if needed).
Only looks on the first page of the document for valid code.
Retrieves only one code per page/document (seems to be the one closest to the center of the page)
