if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # gnu-linux
    apt get install poppler-utils
    add-apt-repository ppa:alex-p/tesseract-ocr-devel
    apt install -y tesseract-ocr
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # Mac OSX
    brew install poppler
    brew install tesseract
else
    echo "No installation instructions for this operating system."
    echo "Please consult the README to install manually."
