/**
 * @license Copyright (c) 2003-2017, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.md or http://ckeditor.com/license
 */
  CKEDITOR.dialog.add( 'videoDialog');
CKEDITOR.plugins.add('video', { init: function (editor) {
    var pluginName = 'video';
    editor.ui.addButton('video', {
         label: 'add video',
         command: 'OpenWindow1',
		 toolbar: 'insert',
         icon: 'http://icons.iconarchive.com/icons/dakirby309/simply-styled/16/YouTube-icon.png'
    });
    var cmd = editor.addCommand('OpenWindow1', new CKEDITOR.dialogCommand( 'videoDialog' ));
}});


CKEDITOR.dialog.add( 'videoDialog', function( editor ) {
    return {
        title: 'Add a video',
        minWidth: 400,
        minHeight: 200,

        contents: [
            {
                id: 'tab-basic',
                label: 'Add a video',
                elements: [
                    {
                        type: 'text',
                        id: 'url',
                        label: 'Video URL',
                        validate: CKEDITOR.dialog.validate.regex( /^(http:\/\/)?(www\.)?((youtube\.com\/watch\?.*?v=.*?)|(youtu\.be\/.*?))$/i, "Please enter a Youtube." )

                    }
                ]
            }
        ],


        onOk: function() {
        var url = editor.document.createElement( 'iframe' );
		var text = this.getValueOf( 'tab-basic', 'url' );
		var normalURL = /^(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/watch.*?\?v=)([^&]*?)$/i;
		var shortURL = /^(?:https?:\/\/)?(?:www\.)?(?:youtu\.be\/)([^&?//]*?)$/i;
		var embedURL = /^(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/embed\/)([^&?//]*?)$/i;
		var match = text.match(normalURL);
		match = match? match : text.match(shortURL) ? text.match(shortURL) : text.match(embedURL);
		var id = "";
		if (match) {
			id = match[1];
			url.setAttribute('_ngcontent-c2','');
			url.setAttribute( 'src', "https://www.youtube.com/embed/" + id );
			editor.insertElement( url );
		}
		

        }
    };
});

CKEDITOR.editorConfig = function( config ) {
	// Define changes to default configuration here.
	// For complete reference see:
	// http://docs.ckeditor.com/#!/api/CKEDITOR.config
	config.enterMode = CKEDITOR.ENTER_BR;
	// The toolbar groups arrangement, optimized for two toolbar rows.
	config.toolbarGroups = [
		{ name: 'clipboard',   groups: [ 'clipboard', 'undo' ] },
		{ name: 'editing',     groups: [ 'find', 'selection', 'spellchecker' ] },
		{ name: 'links' },
		{ name: 'insert' },
		{ name: 'forms' },
		{ name: 'tools' },
		{ name: 'document',	   groups: [ 'mode', 'document', 'doctools' ] },
		{ name: 'others' },
		'/',
		{ name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ] },
		{ name: 'paragraph',   groups: [ 'list', 'indent', 'blocks', 'align', 'bidi' ] },
		{ name: 'styles' }
	];
	// Remove some buttons provided by the standard plugins, which are
	// not needed in the Standard(s) toolbar.
	config.removeButtons = 'Underline,Subscript,Superscript,Styles';
	
	config.format_p={element:"p", name: "Normal",  attributes: { 'class': 'b1' }};
	// Set the most common block elements.
	config.format_tags = 'p';
		CKEDITOR.stylesSet.add( 'my_styles', [
    // Block-level styles
    { name: 'Paragraph22', element: 'p', attributes: { 'class': 'b1' } }

	] );
	
	//config.stylesSet = 'my_styles';

	config.extraPlugins = 'video';
	// Simplify the dialog windows.
	config.removeDialogTabs = 'image:advanced;link:advanced';
};
