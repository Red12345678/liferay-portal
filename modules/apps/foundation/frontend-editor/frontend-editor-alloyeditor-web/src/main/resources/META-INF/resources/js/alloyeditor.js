/* global AlloyEditor */

AUI.add(
	'liferay-alloy-editor',
	function(A) {
		var Do = A.Do;
		var Lang = A.Lang;
		var UA = A.UA;

		var KEY_ENTER = 13;

		var LiferayAlloyEditor = A.Component.create(
			{
				ATTRS: {
					contents: {
						validator: Lang.isString,
						value: ''
					},

					editorConfig: {
						validator: Lang.isObject,
						value: {}
					},

					onBlurMethod: {
						getter: '_getEditorMethod',
						validator: '_validateEditorMethod'
					},

					onChangeMethod: {
						getter: '_getEditorMethod',
						validator: '_validateEditorMethod'
					},

					onFocusMethod: {
						getter: '_getEditorMethod',
						validator: '_validateEditorMethod'
					},

					onInitMethod: {
						getter: '_getEditorMethod',
						validator: '_validateEditorMethod'
					},

					textMode: {
						validator: Lang.isBoolean,
						value: {}
					}
				},

				AUGMENTS: [Liferay.PortletBase],

				EXTENDS: A.Widget,

				NAME: 'liferayalloyeditor',

				NS: 'liferayalloyeditor',

				prototype: {
					initializer: function() {
						var instance = this;

						var editorConfig = instance.get('editorConfig');

						var srcNode = editorConfig.srcNode;

						if (Lang.isString(srcNode)) {
							srcNode = A.one('#' + srcNode);
						}

						instance._alloyEditor = AlloyEditor.editable(srcNode.attr('id'), editorConfig);
						instance._srcNode = srcNode;
					},

					bindUI: function() {
						var instance = this;

						instance._eventHandles = [
							Do.after('_afterGet', instance._srcNode, 'get', instance),
							Do.after('_afterVal', instance._srcNode, 'val', instance)
						];

						var nativeEditor = instance.getNativeEditor();

						nativeEditor.on('dataReady', instance._onDataReady, instance);
						nativeEditor.on('instanceReady', instance._onInstanceReady, instance);
						nativeEditor.on('setData', instance._onSetData, instance);

						if (instance.get('onBlurMethod')) {
							nativeEditor.on('blur', instance._onBlur, instance);
						}

						if (instance.get('onChangeMethod')) {
							nativeEditor.on('change', instance._onChange, instance);
						}

						if (instance.get('onFocusMethod')) {
							nativeEditor.on('focus', instance._onFocus, instance);
						}

						var editorConfig = instance.get('editorConfig');

						if (editorConfig.disallowedContent && editorConfig.disallowedContent.indexOf('br') !== -1) {
							nativeEditor.on('key', instance._onKey, instance);
						}
					},

					destructor: function() {
						var instance = this;

						var editor = instance._alloyEditor;

						if (editor) {
							editor.destroy();
						}

						(new A.EventHandle(instance._eventHandles)).detach();

						instance.instanceReady = false;

						window[instance.get('namespace')].instanceReady = false;
					},

					focus: function() {
						var instance = this;

						instance.getNativeEditor().focus();
					},

					getCkData: function() {
						var instance = this;

						var data = instance.getNativeEditor().getData();

						if (CKEDITOR.env.gecko && CKEDITOR.tools.trim(data) === '<br />') {
							data = '';
						}

						return data;
					},

					getEditor: function() {
						var instance = this;

						return instance._alloyEditor;
					},

					getHTML: function() {
						var instance = this;

						return instance.get('textMode') ? instance.getText() : instance.getCkData();
					},

					getNativeEditor: function() {
						var instance = this;

						return instance._alloyEditor.get('nativeEditor');
					},

					getText: function() {
						var instance = this;

						var editorName = instance.getNativeEditor().name;

						var editor = CKEDITOR.instances[editorName];

						var text = '';

						if (editor) {
							text = editor.editable().getText();
						}

						return text;
					},

					setHTML: function(value) {
						var instance = this;

						if (window[instance.get('namespace')]._dataReady) {
							instance.getNativeEditor().setData(value);
						}
						else {
							instance._pendingData = value;
						}
					},

					_afterGet: function(attrName) {
						var instance = this;

						if (attrName === 'form') {
							var parentForm = instance._parentForm;

							if (!parentForm) {
								parentForm = instance._srcNode.ancestor('form');

								instance._parentForm = parentForm;
							}

							return new Do.AlterReturn(
								'Return ancestor parent form',
								parentForm
							);
						}
						else if (attrName === 'name') {
							return new Do.AlterReturn(
								'Return editor namespace',
								instance.get('namespace')
							);
						}
						else if (attrName === 'type') {
							return new Do.AlterReturn(
								'Return editor node name',
								instance._srcNode.get('nodeName')
							);
						}
					},

					_afterVal: function(value) {
						var instance = this;

						if (value) {
							instance.setHTML(value);
						}

						return new Do.AlterReturn(
							'Return editor content',
							instance.getHTML()
						);
					},

					_getEditorMethod: function(method) {
						return Lang.isFunction(method) ? method : (window[method] || method);
					},

					_onBlur: function(event) {
						var instance = this;

						var blurFn = instance.get('onBlurMethod');

						if (Lang.isFunction(blurFn)) {
							blurFn(event.editor);
						}
					},

					_onChange: function() {
						var instance = this;

						var changeFn = instance.get('onChangeMethod');

						if (Lang.isFunction(changeFn)) {
							changeFn(instance.getText());
						}
					},

					_onDataReady: function(event) {
						var instance = this;

						if (instance._pendingData) {
							var pendingData = instance._pendingData;

							instance._pendingData = null;

							instance.getNativeEditor().setData(pendingData);
						}
						else {
							window[instance.get('namespace')]._dataReady = true;
						}
					},

					_onFocus: function(event) {
						var instance = this;

						var focusFn = instance.get('onFocusMethod');

						if (Lang.isFunction(focusFn)) {
							focusFn(event.editor);
						}
					},

					_onFocusFix: function(activeElement, nativeEditor) {
						var instance = this;

						setTimeout(
							function() {
								nativeEditor.focusManager.blur(true);
								activeElement.focus();
							},
							100
						);
					},

					_onInstanceReady: function() {
						var instance = this;

						var contents = instance.get('contents');

						var editorNamespace = instance.get('namespace');

						if (contents) {
							instance.getNativeEditor().setData(contents);
						}

						var onInitFn = instance.get('onInitMethod');

						if (onInitFn) {
							onInitFn();
						}

						instance.instanceReady = true;

						window[editorNamespace].instanceReady = true;

						Liferay.component(editorNamespace, window[editorNamespace]);

						// LPS-73775

						instance.getNativeEditor().editable().$.addEventListener('compositionend', A.bind('_onChange', instance));

						// LPS-71967 and LPS-75512

						if (contents && UA.edge && parseInt(UA.edge, 10) >= 14) {
							A.soon(
								function() {
									if (document.activeElement && document.activeElement !== document.body) {
										var nativeEditor = instance.getNativeEditor();

										nativeEditor.once('focus', A.bind('_onFocusFix', instance, document.activeElement, nativeEditor));

										nativeEditor.focus();
									}
								}
							);
						}

						// LPS-72963

						var editorConfig = instance.getNativeEditor().config;

						var removeResizePlugin = editorConfig.removePlugins && editorConfig.removePlugins.indexOf('ae_dragresize') != -1;

						if (CKEDITOR.env.gecko && removeResizePlugin) {
							var doc = instance.getNativeEditor().document.$;

							doc.designMode = 'on';

							doc.execCommand('enableObjectResizing', false, false);
							doc.execCommand('enableInlineTableEditing', false, false);

							doc.designMode = 'off';
						}
					},

					_onKey: function(event) {
						if (event.data.keyCode === KEY_ENTER) {
							event.cancel();
						}
					},

					_onSetData: function(event) {
						var instance = this;

						window[instance.get('namespace')]._dataReady = false;
					},

					_validateEditorMethod: function(method) {
						return Lang.isString(method) || Lang.isFunction(method);
					}
				}
			}
		);

		A.LiferayAlloyEditor = LiferayAlloyEditor;
	},
	'',
	{
		requires: ['aui-component', 'liferay-portlet-base', 'timers']
	}
);