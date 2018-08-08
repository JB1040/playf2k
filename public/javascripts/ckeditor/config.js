/**
 * @license Copyright (c) 2003-2017, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.md or http://ckeditor.com/license
 */

CKEDITOR.plugins.add('liTab', {
    init: function(editor) {
        editor.on('key', function(ev) {
            if( ev.data.keyCode == 9 || ev.data.keyCode == CKEDITOR.SHIFT + 9) {
                if ( editor.focusManager.hasFocus )
                {
                    var sel = editor.getSelection(),
                    ancestor = sel.getCommonAncestor();
                    li = ancestor.getAscendant({li:1, td:1, th:1}, true);
                    if(li && li.$.nodeName == 'LI') {
                        editor.execCommand(ev.data.keyCode == 9 ? 'indent' : 'outdent');
                        ev.cancel();
                    }
                    // else we've found a td/th first, so let's not break the
                    // existing tab functionality in table cells.
                }
                
            }
        }, null, null, 5); // high priority (before the tab plugin)
    }
});

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

CKEDITOR.plugins.add('clearfix', { init: function (editor) {
    var pluginName = 'clearfix';
    editor.ui.addButton('clearfix', {
         label: 'Stop image wrapping',
         command: 'insertFix1',
		 toolbar: 'insert',
         icon: 'http://icons.iconarchive.com/icons/dakirby309/simply-styled/16/YouTube-icon.png'
    });
    var cmd = editor.addCommand('insertFix1', {exec:clearFix});
}});

CKEDITOR.plugins.add('deckcode', { init: function (editor) {
    var pluginName = 'deckcode';
    editor.ui.addButton('deckcode', {
         label: 'add deckcode button',
         command: 'openDeckCode',
		 toolbar: 'insert',
         icon: 'https://t4.ftcdn.net/jpg/01/09/83/05/240_F_109830551_1KgnyuoTnZxJzNbywoTt6Reu8ASL5dNx.jpg'
    });
    var cmd = editor.addCommand('openDeckCode', new CKEDITOR.dialogCommand( 'deckcodeDialog' ));
}});

CKEDITOR.plugins.add('twittercode', { init: function (editor) {
    var pluginName = 'twittercode';
    editor.ui.addButton('twittercode', {
         label: 'add twitter button',
         command: 'openTwitter',
		 toolbar: 'insert',
         icon: 'http://ericbrown.com/wp-content/uploads/2013/05/twitter-bird-blue-on-white.png'
    });
    var cmd = editor.addCommand('openTwitter', new CKEDITOR.dialogCommand( 'twitterDialog' ));
}});

function clearFix(e) {
	var newElement = CKEDITOR.dom.element.createFromHtml( '<div class="clearfix" style="clear:both; text-decoration:line-through; font-style:italic;">CLEARFIXLINE - DONT EDIT</div', e.document );
	e.insertElement(newElement);
}
       
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
                        validate: CKEDITOR.dialog.validate.regex( /^(https?:\/\/)?(www\.)?((youtube\.com\/watch\?.*?v=.*?)|(youtu\.be\/.*?))$/i, "Please enter a Youtube." )

                    }
                ]
            }
        ],


        onOk: function() {
        var url = editor.document.createElement( 'iframe' );
		var text = this.getValueOf( 'tab-basic', 'url' );
		var normalURL = /^(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/watch\?.*?v=)([^&]*?)$/i;
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

CKEDITOR.dialog.add( 'deckcodeDialog', function( editor ) {
    return {
        title: 'Add a deckcode',
        minWidth: 400,
        minHeight: 200,

        contents: [
            {
                id: 'tab-deck',
                label: 'deckcode',
                elements: [
                    {
                        type: 'text',
                        id: 'text',
                        label: 'button text',
                        'default':'Copy deck code!'
                    },
                    {
                        type: 'text',
                        id: 'deckcode',
                        label: 'deck code string'
                    }
                ]
            }
            
        ],


        onOk: function() {
        var button = editor.document.createElement( 'button' );
		var text = this.getValueOf( 'tab-deck', 'text' );
		var code = this.getValueOf( 'tab-deck', 'deckcode' );
	
		button.setAttribute('title',code);
		button.setText( text );
		button.addClass("f2kDeckCode");
		editor.insertElement( button );
		
		

        }
    };
});

CKEDITOR.dialog.add( 'twitterDialog', function( editor ) {
    return {
        title: 'Add a twitter message',
        minWidth: 400,
        minHeight: 200,

        contents: [
            {
                id: 'tab-twitter',
                label: 'twitter',
                elements: [
                    {
                        type: 'text',
                        id: 'textT',
                        label: 'twitter URL (Press \'v\' (top right) on tweet and select copy URL)'
                    }
                ]
            }
            
        ],


        onOk: function() {
        var spn= editor.document.createElement( 'span' );
		var text = this.getValueOf( 'tab-twitter', 'textT' );
		$.ajax({
			url:'https://publish.twitter.com/oembed?url=' + text,
			crossDomain: true,
    		dataType: 'jsonp',
    		success: function(data) {
    			editor.insertHtml( data.html );
			}
		});
		
		
		

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
		{ name: 'styles' },
		{ name: 'font' , groups:['size']},
		{ name: 'colors' }
	];
	// Remove some buttons provided by the standard plugins, which are
	// not needed in the Standard(s) toolbar.
	config.removeButtons = 'Subscript,Superscript,Format';
	config.keystrokes =
	[
	    [ 9, 'indent' ],
	    [ CKEDITOR.SHIFT + 9, 'outdent' ]
	];
//	config.format_p={element:"p", name: "Normal",  attributes: { 'class': 'b1' }};
//	config.format_span={element:"span", name: "Section",  attributes: { 'class': 'f2kSpoiler' }};
	// Set the most common block elements.
//	config.format_tags = 'p;span';
	CKEDITOR.stylesSet.add( 'my_styles', [
		 { name: 'Header 1',       element: 'h1'  , styles: {'display':'inline-block'} },
		 { name: 'Header 2',       element: 'h2'  , styles: {'display':'inline-block'} },
		 { name: 'Header 3',       element: 'h3'  , styles: {'display':'inline-block'} },
		 { name: 'Header 4',       element: 'h4'  , styles: {'display':'inline-block'} },
		 { name: 'Header 5',       element: 'h5'  , styles: {'display':'inline-block'} },
		 { name: 'Header 6',       element: 'h6'  , styles: {'display':'inline-block'} },
	] );
	
	config.stylesSet = 'my_styles';
	
	config.extraAllowedContent ='div(*);div{*};span(*);span{*}';
	config.extraPlugins = 'video,indent,justify,font,clearfix,button,panelbutton,panel,floatpanel,dialog,colorbutton,colordialog,deckcode,twittercode,liTab,indentblock';
	// Simplify the dialog windows.
	config.removeDialogTabs = 'image:advanced;link:advanced';
};
