package com.liud.ainocodegenerator.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.liud.ainocodegenerator.exception.BusinessException;
import com.liud.ainocodegenerator.exception.ErrorCode;
import com.liud.ainocodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.Charset;

import static com.liud.ainocodegenerator.constant.AppConstant.CODE_OUTPUT_ROOT_DIR;

public abstract class CodeFileSaverTemplate<T> {


    private static final String FILE_STORAGE_PATH = CODE_OUTPUT_ROOT_DIR;


    public File saveCodeFile(T codeResult, CodeGenTypeEnum codeGenTypeEnum, Long appid){
        // 校验
        checkParam(codeResult, codeGenTypeEnum);
        // 保存文件
        return saveFileCode(codeResult, appid);
    }

    protected abstract File saveFileCode(T codeResult, Long appid);

    protected void checkParam(Object codeResult, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeResult == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "codeResult is null");
        }
    }

    // 构建唯一路径
    protected String generateStoragePath(Long appid) {
        String uniquePath = StrUtil.format("{}_{}", getBzyType(), appid);
        String dirPath = FILE_STORAGE_PATH + File.separator + uniquePath;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    // 保存文件
    protected static void saveFile(String dirPath, String fileName, String fileContent) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(fileContent, filePath, Charset.defaultCharset());
    }

    protected abstract String getBzyType();
}
