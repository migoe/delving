"""
 Copyright 2010 EDL FOUNDATION

 Licensed under the EUPL, Version 1.1 or as soon they
 will be approved by the European Commission - subsequent
 versions of the EUPL (the "Licence");
 you may not use this work except in compliance with the
 Licence.
 You may obtain a copy of the Licence at:

 http://ec.europa.eu/idabc/eupl

 Unless required by applicable law or agreed to in
 writing, software distributed under the Licence is
 distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 express or implied.
 See the Licence for the specific language governing
 permissions and limitations under the Licence.


 Created by: Jacob Lundqvist (Jacob.Lundqvist@gmail.com)



"""

import codecs
import datetime
import subprocess
import sys
import time
import os

import django.db
from django.core import exceptions
from django.conf import settings

from apps.process_monitor.sipproc import SipProcess
from apps.base_item import models as base_item

import models

try:
    TREE_IS_INGESTION_SVN = settings.TREE_IS_INGESTION_SVN
except:
    TREE_IS_INGESTION_SVN = False



IMPORT_SCAN_TREE = settings.IMPORT_SCAN_TREE


AUTOGEN_NAME = 'Autogenerated by Sip Manager'
REC_START = '<record>'
REC_STOP = '</record>'


class RequestCreate(SipProcess):
    SHORT_DESCRIPTION = 'Scan for new imports, if found grab it'
    EXECUTOR_STYLE = True

    PROVIDER_TYPE_LOOKUP = {'L': models.PROVT_LIBRARY,
                            'A':models.PROVT_ARCHIVE,
                            'M': models.PROVT_MUSEUM,
                            'AA': models.PROVT_AUDIO_VIS_ARCH,
                            'AG': models.PROVT_AGGREGATOR,
                            }

    def __init__(self, *args, **kwargs):
        self.skip_dirs = ['error', 'to-import', 'to-import-cache-only',
                          'to-import-no-cache', 'uploading', '.svn']
        super(RequestCreate, self).__init__(*args, **kwargs)

    def run_it(self):
        """
        Dirs to be scanned:
            finished
            imported
            importing
            nonexistent
            uploaded
            validated
            validating

        Indicating a deletion:
            error

        Avoid the following:
            to-import
            to-import-cache-only
            to-import-no-cache
            uploading
        """
        skip_trees = [] # if we find a subtree that shouldnt be followed like .svn dont dive into it
        for dirpath, dirnames, filenames in os.walk(IMPORT_SCAN_TREE):
            if dirpath == IMPORT_SCAN_TREE:
                continue # we dont want this one to end up in skip_trees, that would be lonely...
            b = False
            for bad_tree in skip_trees:
                if bad_tree in dirpath:
                    b = True
                    break
            if b:
                continue

            # avoid specific relative dirs that shouldnt be traversed
            if os.path.split(dirpath)[-1] in self.skip_dirs:
                skip_trees.append(dirpath)
                continue

            # if we are scanning the ingestion svn only /.../output_xml/ should be used
            if TREE_IS_INGESTION_SVN and os.path.split(dirpath)[-1] != 'output_xml':
                continue

            for filename in filenames:
                if os.path.splitext(filename)[1] != '.xml':
                    continue
                # if we are scanning the ingestion svn avoid things like 'dddd.sample.xml'
                if TREE_IS_INGESTION_SVN and os.path.splitext(os.path.splitext(filename)[0])[1] != '':
                    # extra check needed for using ingestion svn tree
                    continue

                # we (hopefully) have a relevant file, check if it is already ingested
                data_sets = models.DataSet.objects.filter(name_code=filename)
                if data_sets:
                    data_set = data_sets[0]
                else:
                    # in normal operation we should propably abort here,
                    # Datasets should propably be pre-created by ingestion
                    data_set = self.add_data_set(filename)
                    if not data_set:
                        continue # wasnt a valid dataset

                full_path = os.path.join(dirpath, filename)
                mtime = os.path.getmtime(full_path)
                time_created = datetime.datetime.fromtimestamp(mtime)
                requests = models.Request.objects.filter(data_set=data_set,
                                                         time_created=time_created,
                                                         file_name=filename)
                if not requests:
                    # only add new requests
                    record_count = self.record_count(full_path)
                    request = models.Request().add_from_file(data_set, full_path, record_count)
                    self.log('Added request %s' % filename, 3)
                pass
        return True

    def add_data_set(self, file_name):
        try:
            int(file_name[:5]) # we use it as string but check that it is an int...
        except:
            return None # not a valid dataset

        aggregator_nc = file_name[:2]
        aggregators = models.Aggregator.objects.filter(name_code=aggregator_nc)
        if aggregators:
            aggregator = aggregators[0]
        else:
            aggregator = models.Aggregator(name_code=aggregator_nc, name=AUTOGEN_NAME)
            aggregator.save()

        provider_nc = file_name[2:5]
        providers = models.Provider.objects.filter(name_code=provider_nc)
        if providers:
            provider = providers[0]
        else:
            try:
                sanitized_file_name = os.path.splitext(file_name)[0].replace('__','_')
                s = sanitized_file_name.split('_')[1].upper()
                item_type = self.PROVIDER_TYPE_LOOKUP[s]
                country_idx = 2
            except:
                item_type = 1
                country_idx= 1
            try:
                country = sanitized_file_name.split('_')[country_idx]
                if len(country) > 2:
                    raise
            except:
                country = '??'
            try:
                name = ' '.join(sanitized_file_name.split('_')[country_idx+1:])
            except:
                name = AUTOGEN_NAME
            provider = models.Provider(aggregator=aggregator,
                                       name_code=provider_nc,
                                       name=name,
                                       item_type=item_type,
                                       country=country)
            provider.save()

        data_sets = models.DataSet.objects.filter(name_code=file_name)
        if data_sets:
            data_set = data_sets[0]
        else:
            data_set = models.DataSet(provider=provider,
                                      name_code=file_name,
                                      name=AUTOGEN_NAME,
                                      language='??')
            data_set.save()

        return data_set

    def record_count(self, fname):
        #cmd = "awk 'BEGIN {RS=FS}{if ( $0 ~ /<record>/ ) c++} END{print c}' %s" % fname
        cmd = 'awk -F "<record>" \'{s+=(NF-1)} END {print s}\' %s' % fname
        self.log('counting records in %s' % fname, 9)
        p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE, close_fds=True)
        retcode = p.wait()
        if retcode:
            msg = 'shell command counting records in xml-file failed: %s' % fname
            self.log('*****************************',1)
            self.log('Aborting due to failure with %s.record_count()' % __name__, 1)
            self.log(msg,1)
            sys.exit(1)
            #self.abort_process(msg)
        rec_count = p.stdout.read().strip() or 0
        self.log('\tfound records: %s' % rec_count, 9)
        return rec_count



class RequestParseNew(SipProcess):
    SHORT_DESCRIPTION = 'Parse new Requests'
    EXECUTOR_STYLE = True

    def prepare(self):
        try:
            request = models.Request.objects.filter(status=models.REQS_PRE,
                                                        pid=0)[0]
        except:
            return False

        # in order not to grab control to long, just handle one request on each call to this
        self.request_id = request.id
        return True


    def run_it(self):
        try:
            req = models.Request.objects.filter(pk=self.request_id,pid=0)[0]
        except:
            # either request was deleted or taken by somebody else
            return False
        request = self.grab_item(models.Request, req.pk,
                                 'About to parse for ese records')
        if not request:
            return False # Failed to take control of it

        self.current_request = request # make it available without params for other modules
        full_path = self.find_file()
        if not full_path:
            return self.request_failure('Cant find file %s for Request %i' % (
                request.file_name, request.pk))

        self.log('Parsing ese file for records: %s' % full_path, 1)
        f = open(full_path, 'r')
        record = []
        self.task_starting('Reading ESE records from file (req:%i)' % request.pk,request.record_count)
        line = f.readline()[:-1].strip() # skip lf and other pre/post whitespace
        record_count = 0
        t0 = time.time()
        while line:
            if line == REC_START:
                record = []
                record_count += 1
            elif line == REC_STOP:
                record.sort()

                # start and stop tags shouldnt be sorted so add them after
                record.insert(0, REC_START)
                record.append(REC_STOP)

                record_str = '\n'.join(record)
                r_hash = base_item.calculate_mdr_content_hash(record_str)
                try:
                    mdr = base_item.MdRecord(content_hash=r_hash,source_data=record_str)
                    mdr.save()
                except django.db.IntegrityError: # asume record exists
                    mdrs = base_item.MdRecord.objects.filter(content_hash=r_hash)
                    if len(mdrs):
                        mdr = mdrs[0]
                    else:
                        pass
                r_m = base_item.RequestMdRecord(request=request,
                                                 md_record=mdr)
                r_m.save()
            elif line: # skip empty lines
                record.append(line)
            line = f.readline()[:-1].strip() # skip lf and other pre/post whitespace
            if t0 + self.TASK_PROGRESS_TIME < time.time():
                self.task_progress(record_count)
                t0 = time.time()
        f.close()
        request.status = models.REQS_INIT
        request.save()
        self.release_item(models.Request, request.pk)
        return True


    def add_record(self, record):
        record_s = '\n'.join(record.sort())
        r_hash = base_item.calculate_mdr_content_hash(record_s)
        mdrs = base_item.MdRecord.objects.filter(content_hash=r_hash)
        if mdrs:
            mdr= mdrs[0]
        else:
            # MdRecord not found, create a new
            mdr = base_item.MdRecord(content_hash=r_hash,source_data=record_s)
            mdr.save()
        r_m = base_item.RequestMdRecord(request=self.current_request,
                                         md_record=mdr)
        r_m.save()

    def find_file(self):
        ret = ''
        found = False
        for dirpath, dirnames, filenames in os.walk(IMPORT_SCAN_TREE):
            if found:
                break
            for filename in filenames:
                if filename == self.current_request.file_name:
                    full_path = os.path.join(dirpath, filename)
                    mtime = os.path.getmtime(full_path)
                    time_created = datetime.datetime.fromtimestamp(mtime)
                    if self.current_request.time_created == time_created:
                        found = True
                        ret = full_path
                        break
                    pass
            pass
        return ret


    def request_failure(self, msg):
        self.current_request.status = models.REQS_ABORTED
        self.current_request.save()
        self.release_item(models.Request, self.current_request.pk)
        self.current_request = None
        self.error_log(msg)
        return False # propagate error





task_list = [RequestCreate,
             RequestParseNew
             ]
