(ns org.jetbrains.plugins.clojure.utils.clojure-utils
  (:use [org.jetbrains.plugins.clojure.utils.java-wrappers])
  (:import [com.intellij.psi PsiFile PsiElement PsiReference PsiDocumentManager]
   [org.jetbrains.plugins.clojure.psi.util ClojurePsiFactory ClojurePsiUtil]
   [org.jetbrains.plugins.clojure.parser ClojureElementTypes ClojurePsiCreator]
   [com.intellij.openapi.editor Editor]
   [org.jetbrains.plugins.clojure.file ClojureFileType]
   [com.intellij.lang ASTNode]
   [com.intellij.psi.tree IElementType]
   [org.jetbrains.plugins.clojure.psi.api ClojureFile ClVector ClList]
   [org.jetbrains.plugins.clojure.psi.api.defs ClDef]
   [org.jetbrains.plugins.clojure ClojureBundle]
   [com.intellij.refactoring.util CommonRefactoringUtil]
   [com.intellij.openapi.project Project]
   [org.jetbrains.plugins.clojure.psi ClojurePsiElement]
   [com.intellij.psi.util PsiTreeUtil]
   [com.intellij.openapi.util TextRange]
   [com.intellij.openapi.command CommandProcessor]))


(defn show-error
  [project editor msg]
  (CommonRefactoringUtil/showErrorHint
    project
    editor
    msg
    msg
    nil))


(defn bundle-message
  [msg-name]
  (ClojureBundle/message msg-name (into-array [])))


(defmacro maybe
  [f]
  `(fn [arg#]
     (if-let [x# arg#]
       (~f x#))))


(defn with-error!
  [pred project editor msg]
  (fn
    [& args]
    (if-let [answer (apply pred args)]
      answer
      (show-error project editor msg))))


(defn n-iter
  [x n f]
  (nth
    (iterate
      (maybe f)
      x)
    n))

(defn find-element-by-offset
  [^PsiFile file offset]
  (PsiTreeUtil/findElementOfClassAtOffset
    file
    offset
    ClojurePsiElement
    true))


(defn get-caret-psi-ref
  [editor file]
  (find-ref-by-offset
    file
    (get-current-position editor)))

(defn is-clojure?
  [^PsiFile file]
  (let [lang (.getLanguage file)]
    (=
      lang
      (ClojureFileType/CLOJURE_LANGUAGE))))

(defn instance-of?
  [^PsiElement psi ^IElementType expected]
  (=
    (some-> psi
      .getNode
      .getElementType)
    expected))


(defn is-quoted?
  [^PsiElement psi]
  (instance-of?
    psi
    (ClojureElementTypes/QUOTED_FORM)))

(defn is-symbol?
  [^PsiElement psi]
  (instance-of?
    psi
    (ClojureElementTypes/SYMBOL)))

(defn is-def?
  [psi]
  (instance-of?
    psi
    (ClojureElementTypes/DEF)))


(defn ^PsiElement lift
  [^PsiElement psi by-n]
  (n-iter
    psi
    by-n
    (fn [^PsiElement psi] (.getParent psi))))

(defn ^PsiElement right-down
  [^PsiElement psi by-n]
  (n-iter
    psi
    by-n
    (fn [^PsiElement psi] (last (.getChildren psi)))))

(defn ^PsiElement left-down
  [^PsiElement psi by-n]
  (n-iter
    psi
    by-n
    (fn [^PsiElement psi] (first (.getChildren psi)))))

(defn create-symbol-node-from-text
  [project text]
  (.createSymbolNodeFromText
    (ClojurePsiFactory/getInstance project)
    text))

(defn check-def
  [^ClDef def check-name check-ns]
  (and
    (=
      (.getDefinedName def)
      check-name)
    (=
      (some-> def
        .getContainingFile
        get-namespace)
      check-ns)))

(defn is-let-form?
  [^ClojurePsiElement form]
  (=
    "let"
    (some-> (ClojurePsiUtil/firstChildSexp form)
      (.getText))))

(defn get-let-body-expr
  [^PsiElement let-form]
  (some->> let-form
    get-children
    (drop 2)))

(defn ^ClVector get-let-bindings
  [^PsiElement let-form]
  (PsiTreeUtil/getChildOfType
    let-form
    ClVector))

(defn psi-tree-seq
  ([^PsiElement element branch?]
      (tree-seq branch? get-children element))
  ([^PsiElement element]
    (letfn [(branch?
              [e]
              (some-> e
                get-children
                empty?
                false?))]
      (tree-seq branch? get-children element))))








